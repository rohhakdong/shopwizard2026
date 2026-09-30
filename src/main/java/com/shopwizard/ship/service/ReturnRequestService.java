package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.ship.mapper.ReturnDirectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 배송완료 건 1개를 반품요청으로 전환한다 (배송완료 → 반품요청).
 * 순서가 중요하다: {@link ReturnDirectMapper#insertReturnDirect}의 SELECT-INSERT가
 * {@code tOrdOrderProd.ReturnReason/ReturnReasonDetail/ReturnReciptDate}를 그대로 복사해가므로,
 * INSERT 전에 반드시 원본 라인에 사유를 먼저 기록해야 한다. 또한 이 INSERT는
 * {@code WHERE OP.OrderState = '배송완료'} 가드가 있으므로, 사유 기록 시점에는 아직 라인 상태를
 * '반품요청'으로 바꾸면 안 된다(가드가 막혀버림) — 상태 전환은 INSERT 성공 뒤에 한다.
 *
 * <p>ShipDirectService(오케스트레이션, requestBatch를 옮겨 받은 ReturnDirectService)와 다른
 * 빈으로 분리한 이유는 건별 {@code REQUIRES_NEW} 격리를 위해서다.</p>
 */
@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnDirectMapper returnDirectMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderMapper orderMapper;

    /** 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String requestOne(Integer orderNo, Integer orderProdNo, String returnReason, String returnReasonDetail,
                              String registId, String registName) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("orderNo", orderNo);
        raw.put("orderProdNo", orderProdNo);
        OrderProd op = orderProdMapper.selectLineRaw(raw);
        if (op == null) {
            return "주문라인을 찾을 수 없습니다.";
        }
        if (!"배송완료".equals(op.getOrderState())) {
            return "배송완료 상태가 아닙니다. (현재 상태: " + op.getOrderState() + ")";
        }

        OrderProd reasonUpd = new OrderProd();
        reasonUpd.setOrderNo(orderNo);
        reasonUpd.setOrderProdNo(orderProdNo);
        reasonUpd.setReturnReciptDate("Y");
        reasonUpd.setReturnReason(returnReason);
        reasonUpd.setReturnReasonDetail(returnReasonDetail);
        reasonUpd.setChangeId(registId);
        reasonUpd.setChangeName(registName);
        orderProdMapper.updateReturnReason(reasonUpd);

        Map<String, Object> ins = new HashMap<>();
        ins.put("orderNo", orderNo);
        ins.put("orderProdNo", orderProdNo);
        ins.put("orderChangeNo", 1);
        ins.put("orderType", "정상주문");
        ins.put("registId", registId);
        ins.put("registName", registName);
        int inserted = returnDirectMapper.insertReturnDirect(ins);
        if (inserted == 0) {
            return "배송완료 상태가 아니거나 이미 반품요청된 건입니다.";
        }

        Map<String, Object> stateUpd = new HashMap<>();
        stateUpd.put("orderNo", orderNo);
        stateUpd.put("orderProdNo", orderProdNo);
        stateUpd.put("orderState", "반품요청");
        stateUpd.put("changeId", registId);
        stateUpd.put("changeName", registName);
        orderProdMapper.updateOrderState(stateUpd);

        Map<String, Object> diff = new HashMap<>();
        diff.put("orderNo", orderNo);
        diff.put("orderState", "반품요청");
        if (orderProdMapper.countOrderStateDiffer(diff) == 0) {
            Map<String, Object> headerUpd = new HashMap<>();
            headerUpd.put("orderNo", orderNo);
            headerUpd.put("orderState", "반품요청");
            headerUpd.put("changeId", registId);
            headerUpd.put("changeName", registName);
            orderMapper.updateOrderState(headerUpd);
        }

        return null;
    }
}
