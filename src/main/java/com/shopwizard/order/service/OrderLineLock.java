package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 주문 라인 상태 전이를 직렬화하는 행 잠금. 반드시 호출자의 <b>이미 열린 트랜잭션 안에서</b> 호출한다(잠금은 커밋/롤백까지 유지).
 *
 * <p>그동안 건별 서비스는 "잠금 없는 SELECT 로 상태 확인 → 무조건 UPDATE" 였다. 그래서 같은 라인을 두 요청이 동시에 처리하면
 * 서로의 결과를 덮어썼고(취소가 이미 배송시작된 라인을 주문취소로 되돌림), 같은 주문의 라인 두 개를 동시에 취소하면 각 트랜잭션이
 * 자기 스냅샷에서 "아직 남은 라인이 있다"고 판단해 헤더가 영영 승격되지 않았다(write-skew).</p>
 *
 * <p><b>잠금 순서 규칙(교착 방지)</b>: 주문 헤더를 건드리는 전이(취소·환불확정·반품요청·반품확정·토스 웹훅)는
 * {@link #lockHeaderAndLine} 으로 <b>헤더 → 라인</b> 순서로 잠근다. 헤더를 건드리지 않는 전이(출고지시·발주확인·송장입력·
 * 배송완료)는 {@link #lockLine} 만 쓴다 — 헤더를 기다리지 않으므로 잠금 순환이 생기지 않는다. 잠금 읽기(FOR UPDATE)는
 * 스냅샷을 만들지 않아서, 잠근 뒤의 일반 SELECT(countOrderStateDiffer 등)는 앞선 트랜잭션이 커밋한 최신 상태를 본다.</p>
 */
@Component
@RequiredArgsConstructor
public class OrderLineLock {

    private final OrderProdMapper orderProdMapper;
    private final OrderMapper orderMapper;

    /** 주문 헤더 행을 잠근다. 헤더가 없으면 아무것도 잠기지 않는다(호출자가 라인 조회에서 없음으로 처리). */
    public void lockHeader(Integer orderNo) {
        if (orderNo != null) orderMapper.selectHeaderForUpdate(orderNo);
    }

    /** 라인 행을 잠그고 최신 상태의 라인을 돌려준다. 없으면 null. */
    public OrderProd lockLine(Integer orderNo, Integer orderProdNo) {
        Map<String, Object> key = new HashMap<>();
        key.put("orderNo", orderNo);
        key.put("orderProdNo", orderProdNo);
        return orderProdMapper.selectLineForUpdate(key);
    }

    /** 헤더 → 라인 순서로 잠근다. */
    public OrderProd lockHeaderAndLine(Integer orderNo, Integer orderProdNo) {
        lockHeader(orderNo);
        return lockLine(orderNo, orderProdNo);
    }
}
