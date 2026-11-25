package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수수료 규칙 계산 결과를 주문 라인 1건에 쓴다. 건별 {@code REQUIRES_NEW} 라 한 건이 실패해도 나머지는 그대로 반영되고
 * (출고지시·발주확인과 같은 패턴), 라인을 잠근 뒤 아직 지불완료·같은 상점인지 다시 확인한다 — 그 사이 출고지시·취소된 라인은 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
public class OrderFeeLineWriter {

    /** 계산된 새 값(배송형태·배송비·판촉비·공급가). */
    public record Values(String deliFeeType, int deliFeeAmt, int promotFeeAmt, int supplyPrice) {}

    private final OrderLineLock lineLock;
    private final OrderProdMapper orderProdMapper;

    /** @return 반영했으면 true, 라인이 바뀌어 건너뛰었으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean write(Integer orderNo, Integer orderProdNo, String shopCode, Values v, String actorId, String actorName) {
        OrderProd op = lineLock.lockLine(orderNo, orderProdNo);
        if (op == null || !"지불완료".equals(op.getOrderState())) return false;
        if (shopCode != null && !shopCode.equals(op.getShopCode())) return false;

        int sale = nvl(op.getSalePrice());
        int nv = nvl(op.getNvPrice());
        int buy = nvl(op.getBuyPrice());
        op.setDeliFeeType(v.deliFeeType());
        op.setDeliFeeAmt(v.deliFeeAmt());
        op.setPromotFeeAmt(v.promotFeeAmt());
        op.setSupplyPrice(v.supplyPrice());
        op.setProdMargin(sale + nv - buy);
        op.setChnlMargin(sale + nv - v.supplyPrice());
        op.setNetMargin(v.supplyPrice() - buy - v.promotFeeAmt());
        op.setChangeId(actorId);
        op.setChangeName(actorName);
        orderProdMapper.updatePrice(op);
        return true;
    }

    private static int nvl(Integer i) { return i == null ? 0 : i; }
}
