package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 주문라인 1건을 환불완료로 전환한다 (주문취소 또는 반품완료 → 환불완료).
 * 별도 스테이징 테이블 없이 {@code tOrdOrderProd}/{@code tOrdOrder} 상태만 바로 바꾼다.
 * 원본 상태 확인은 {@link OrderProdMapper#selectLineRaw}로 직접 한다(대상 매퍼에 origin-state
 * WHERE 가드가 없음). 그 주문의 모든 라인이 환불완료에 도달하면 헤더도 승격한다
 * ({@link OrderProdMapper#countOrderStateDiffer}).
 *
 * <p>OrderService(오케스트레이션, confirmRefundBatch)와 다른 빈으로 분리한 이유는 건별
 * {@code REQUIRES_NEW} 격리를 위해서다.</p>
 */
@Service
@RequiredArgsConstructor
public class RefundConfirmService {

    private static final Set<String> REFUNDABLE_STATES = Set.of("주문취소", "반품완료");

    private final OrderMapper orderMapper;
    private final OrderProdMapper orderProdMapper;

    /** 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String confirmOne(Integer orderNo, Integer orderProdNo, String registId, String registName) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("orderNo", orderNo);
        raw.put("orderProdNo", orderProdNo);
        OrderProd op = orderProdMapper.selectLineRaw(raw);
        if (op == null) {
            return "주문라인을 찾을 수 없습니다.";
        }
        if (!REFUNDABLE_STATES.contains(op.getOrderState())) {
            return "환불확정 가능한 상태가 아닙니다. (현재 상태: " + op.getOrderState() + ")";
        }

        Map<String, Object> upd = new HashMap<>();
        upd.put("orderNo", orderNo);
        upd.put("orderProdNo", orderProdNo);
        upd.put("orderState", "환불완료");
        upd.put("changeId", registId);
        upd.put("changeName", registName);
        orderProdMapper.updateOrderState(upd);

        Map<String, Object> diff = new HashMap<>();
        diff.put("orderNo", orderNo);
        diff.put("orderState", "환불완료");
        if (orderProdMapper.countOrderStateDiffer(diff) == 0) {
            Map<String, Object> headerUpd = new HashMap<>();
            headerUpd.put("orderNo", orderNo);
            headerUpd.put("orderState", "환불완료");
            headerUpd.put("changeId", registId);
            headerUpd.put("changeName", registName);
            orderMapper.updateOrderState(headerUpd);
        }

        return null;
    }
}
