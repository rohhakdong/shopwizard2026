package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.UpriceMatchDetail;
import com.shopwizard.order.service.OrderLineLock;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주문라인 1건을 출고지시로 전환한다 (지불완료 → 출고지시).
 * 순서가 중요하다: {@link ShipDirectMapper#insertShipDirect} 의 WHERE 절이
 * {@code OP.OrderState = '지불완료'} 를 요구하므로, 원본 상태를 먼저 바꾸면 안 된다.
 * 0) 발주단가매칭(shopwizard.tOrdUpriceMatch) 규칙이 없으면 여기서 막는다 — 규칙이 있으면
 *    그 시점 발주단가로 BuyPrice/마진을 다시 계산해 원본 라인에 반영한다(항상 최신 규칙 적용).
 * 1) tShpShipDirect 로 주문데이터 이전(OrderState='출고지시', ShipDirectDate=NOW() 로 SELECT-INSERT)
 *    — 방금 갱신한 BuyPrice 를 그대로 복사해가므로 별도 반영 작업이 필요 없다.
 * 2) 원본 tOrdOrderProd.OrderState 를 '출고지시' 로 갱신 (ShipDirectDate 도 같이 채워짐)
 *
 * <p>ShipDirectService(오케스트레이션, issueBatch)와 다른 빈으로 분리한 이유는 건별
 * {@code REQUIRES_NEW} 격리를 위해서다 — 같은 클래스 안에서 호출하면 프록시를 안 거쳐
 * 트랜잭션 전파가 적용되지 않는다(샵링커 수집의 ShoplinkerOrderWriter 와 같은 패턴).</p>
 */
@Service
@RequiredArgsConstructor
public class ShipDirectIssueService {

    private final ShipDirectMapper shipDirectMapper;
    private final OrderProdMapper orderProdMapper;
    private final UpriceMatchMapper upriceMatchMapper;
    private final OrderLineLock lineLock;   // 상태 확인~갱신 사이에 다른 요청이 끼어들지 못하게 행 잠금

    /** 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String issueOne(Integer orderNo, Integer orderProdNo, String registId, String registName) {
        // 라인을 잠그고 상태를 확인한다. 상태 확인 전에 가격(updatePrice)을 먼저 갱신하면, 이미 발주확인·배송된 라인을
        // 다시 출고지시해도 BuyPrice/마진이 덮여 버린다(실패 메시지를 돌려줘도 이 트랜잭션은 커밋됨).
        OrderProd op = lineLock.lockLine(orderNo, orderProdNo);
        if (op == null) {
            return "주문라인을 찾을 수 없습니다.";
        }
        if (!"지불완료".equals(op.getOrderState())) {
            return "지불완료 상태가 아니거나 이미 출고지시 처리된 주문입니다. (현재 상태: " + op.getOrderState() + ")";
        }

        List<UpriceMatchDetail> priced = upriceMatchMapper.selectPriced(nz(op.getProdName()), nz(op.getItemName()));
        if (priced.isEmpty()) {
            return "발주단가매칭이 필요합니다. (상품명: " + nz(op.getProdName()) + " / 옵션: " + nz(op.getItemName()) + ")";
        }

        // 주문 라인의 금액은 전부 "수량이 곱해진 라인 합계"다(판매가·공급가·원가 공통, 실데이터로 확인). 발주단가 규칙의
        // Σ(규칙수량×단가)는 상품 1개분이므로 주문 수량을 곱해야 정산·마진이 맞는다.
        long buyPerUnit = priced.stream().mapToLong(d -> (long) nvl(d.getQty()) * nvl(d.getUprice())).sum();
        long buyTotal = buyPerUnit * Math.max(1, nvl(op.getProdQty()));
        if (buyTotal > Integer.MAX_VALUE) {
            return "원가 합계가 너무 큽니다. 수량과 발주단가매칭을 확인하세요. (상품명: " + nz(op.getProdName()) + ")";
        }
        int totalBuyPrice = (int) buyTotal;
        int salePrice = nvl(op.getSalePrice());
        int nvPrice = nvl(op.getNvPrice());
        int supplyPrice = nvl(op.getSupplyPrice());
        int promotFee = nvl(op.getPromotFeeAmt());

        op.setBuyPrice(totalBuyPrice);
        op.setProdMargin(salePrice + nvPrice - totalBuyPrice);
        op.setChnlMargin(salePrice + nvPrice - supplyPrice);
        op.setNetMargin(supplyPrice - totalBuyPrice - promotFee);
        op.setChangeId(registId);
        op.setChangeName(registName);
        orderProdMapper.updatePrice(op);

        Map<String, Object> ins = new HashMap<>();
        ins.put("orderNo", orderNo);
        ins.put("orderProdNo", orderProdNo);
        ins.put("orderChangeNo", 1);
        ins.put("orderType", "정상주문");
        ins.put("registId", registId);
        ins.put("registName", registName);

        int inserted = shipDirectMapper.insertShipDirect(ins);
        if (inserted == 0) {
            return "지불완료 상태가 아니거나 이미 출고지시 처리된 주문입니다.";
        }

        Map<String, Object> upd = new HashMap<>();
        upd.put("orderNo", orderNo);
        upd.put("orderProdNo", orderProdNo);
        upd.put("orderState", "출고지시");
        upd.put("changeId", registId);
        upd.put("changeName", registName);
        orderProdMapper.updateOrderState(upd);

        return null;
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static int nvl(Integer i) { return i == null ? 0 : i; }
}
