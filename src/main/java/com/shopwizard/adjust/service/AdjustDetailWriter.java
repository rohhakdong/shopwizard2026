package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustDetailMapper;
import com.shopwizard.adjust.mapper.AdjustMapper;
import com.shopwizard.adjust.mapper.AdjustProcessMapper;
import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.model.AdjustDetail;
import com.shopwizard.adjust.model.AdjustNoSeq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 정산자료 생성·취소·수정과 상점 마감의 건별 처리. 건별 {@code REQUIRES_NEW} 로 독립 커밋해서 한 건이 실패해도 나머지에
 * 영향이 없게 한다(출고지시의 ShipDirectIssueService 와 같은 패턴 — 오케스트레이터와 다른 빈이어야 프록시를 거친다).
 *
 * <p>이중 정산 방지의 핵심은 <b>조건부 UPDATE 의 영향 행 수</b>다: 원본 배송/반품 행의 AdjustSelectDate 가 비어 있을 때만 찍히고
 * (영향 1행) 그때만 상세를 복사한다. 두 요청이 동시에 와도 먼저 찍은 쪽만 1행을 얻는다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdjustDetailWriter {

    public static final String SHIP = "SHIP";
    public static final String RETURN = "RETURN";

    /** 마감할 정산자료가 없다 — 던져서 방금 만든 정산(빈 행)과 번호 채번을 같이 롤백한다. */
    public static class NothingToCloseException extends RuntimeException {
        public NothingToCloseException(String message) { super(message); }
    }

    private final AdjustProcessMapper mapper;
    private final AdjustMapper adjustMapper;
    private final AdjustDetailMapper adjustDetailMapper;

    /** 성공하면 null, 업무상 실패는 사유 문자열. 상세 복사가 1행이 아니면 예외로 전체(선택일 표시 포함) 롤백. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String createOne(String kind, Integer orderNo, Integer orderProdNo, Integer orderChangeNo,
                            String changeId, String changeName) {
        if (orderNo == null || orderProdNo == null || orderChangeNo == null) return "대상 키가 올바르지 않습니다.";
        Map<String, Object> p = key(orderNo, orderProdNo, orderChangeNo, changeId, changeName);
        int marked;
        int copied;
        if (SHIP.equals(kind)) {
            marked = mapper.markShipSelected(p);
            if (marked == 0) return "이미 정산자료로 선택됐거나 정산 대상(배송완료 건)이 아닙니다.";
            copied = mapper.insertDetailFromShip(p);
        } else if (RETURN.equals(kind)) {
            marked = mapper.markReturnSelected(p);
            if (marked == 0) return "이미 정산자료로 선택됐거나 정산 대상(반품완료 건)이 아닙니다.";
            copied = mapper.insertDetailFromReturn(p);
        } else {
            return "구분(kind)이 올바르지 않습니다.";
        }
        if (copied != 1) {
            throw new IllegalStateException("정산자료 복사 결과가 1건이 아닙니다 (" + copied + ")");
        }
        return null;
    }

    /** 마감 전 정산자료를 지우고 원본의 선택일을 풀어 다시 정산 대상으로 되돌린다. 성공하면 null. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String cancelOne(Integer adjustDetailNo, String changeId, String changeName) {
        AdjustDetail d = adjustDetailNo == null ? null : mapper.selectDetailForUpdate(adjustDetailNo);
        if (d == null) return "정산자료를 찾을 수 없습니다.";
        if (d.getBuyAdjustCloseDate() != null || d.getAdjustNo() != null) return "이미 마감된 정산자료는 취소할 수 없습니다.";
        // 반품(−) 자료는 수량이 음수로 복사된다 — 수량 부호로 원본(배송/반품)을 가른다.
        boolean isReturn = d.getProdQty() != null && d.getProdQty() < 0;
        Map<String, Object> p = key(d.getOrderNo(), d.getOrderProdNo(), d.getOrderChangeNo(), changeId, changeName);
        int unmarked = isReturn ? mapper.unmarkReturnSelected(p) : mapper.unmarkShipSelected(p);
        if (unmarked != 1) {
            throw new IllegalStateException("원본 선택일을 풀지 못했습니다 (" + unmarked + ")");
        }
        if (mapper.deleteOpenDetail(adjustDetailNo) != 1) {
            throw new IllegalStateException("정산자료 삭제에 실패했습니다.");
        }
        return null;
    }

    /** 마감 전 정산자료의 금액을 보정한다. 마감됐거나 없으면 false. */
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePrices(Integer adjustDetailNo, Long supplyPrice, Long discntSupplyPrice, Long buyPrice,
                                Integer vatRate, String changeId, String changeName) {
        Map<String, Object> p = new HashMap<>();
        p.put("adjustDetailNo", adjustDetailNo);
        p.put("supplyPrice", supplyPrice);
        p.put("discntSupplyPrice", discntSupplyPrice);
        p.put("buyPrice", buyPrice);
        p.put("vatRate", vatRate);
        p.put("changeId", changeId);
        p.put("changeName", changeName);
        return mapper.updatePricesOpen(p) == 1;
    }

    /**
     * 상점 1곳을 기간 단위로 마감한다 — 상점 행 잠금 → 정산번호 채번 → tAdjAdjust 행 → 그 상점·기간의 미마감 상세에 정산번호와
     * 마감일 기록. 마감할 상세가 0건이면 예외로 전부 롤백한다(빈 정산 행이 남지 않게).
     *
     * @return 발급한 정산번호
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int closeShop(String shopCode, String startDate, String endDate, String svcCode,
                         String changeId, String changeName) {
        Map<String, Object> shop = mapper.selectShopForClose(shopCode);
        if (shop == null) throw new NothingToCloseException("존재하지 않는 상점입니다.");
        String shopSvc = shop.get("svcCode") == null ? null : String.valueOf(shop.get("svcCode"));
        if (svcCode != null && !svcCode.isBlank() && shopSvc != null && !svcCode.equals(shopSvc)) {
            throw new NothingToCloseException("다른 서비스의 상점은 마감할 수 없습니다.");
        }

        // 마감할 자료가 있을 때만 정산번호를 채번한다(상점 행을 잠근 상태라 센 값과 마감 대상이 어긋나지 않는다).
        if (mapper.countClosableDetails(shopCode, startDate, endDate) == 0) {
            throw new NothingToCloseException("마감할 정산자료가 없습니다.");
        }

        AdjustNoSeq seq = new AdjustNoSeq();
        mapper.insertAdjustNo(seq);
        int adjustNo = seq.getSeq();

        Adjust adjust = new Adjust();
        adjust.setAdjustNo(adjustNo);
        adjust.setSupplyCode(shop.get("supplyCode") == null ? null : String.valueOf(shop.get("supplyCode")));
        adjust.setShopCode(shopCode);
        adjust.setAdjustStartDate(startDate);
        adjust.setAdjustEndDate(endDate);
        adjust.setAdjustPeriodCode(shop.get("adjustPeriodCode") == null ? null : String.valueOf(shop.get("adjustPeriodCode")));
        adjust.setState(1);   // 정산중 — 세금계산서 확인 후 화면에서 정산완료(2)로
        adjust.setRegistId(changeId);
        adjust.setRegistName(changeName);
        adjustMapper.insert(adjust);

        Map<String, Object> p = new HashMap<>();
        p.put("adjustNo", adjustNo);
        p.put("pShopCode", shopCode);
        p.put("pStartDate", startDate);
        p.put("pEndDate", endDate);
        p.put("changeId", changeId);
        p.put("changeName", changeName);
        int closed = adjustDetailMapper.updateShopBuy(p);
        if (closed == 0) throw new NothingToCloseException("마감할 정산자료가 없습니다.");
        return adjustNo;
    }

    private static Map<String, Object> key(Integer orderNo, Integer orderProdNo, Integer orderChangeNo,
                                           String changeId, String changeName) {
        Map<String, Object> p = new HashMap<>();
        p.put("orderNo", orderNo);
        p.put("orderProdNo", orderProdNo);
        p.put("orderChangeNo", orderChangeNo);
        p.put("changeId", changeId);
        p.put("changeName", changeName);
        return p;
    }
}
