package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderLineLock;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 출고지시 건 1개를 발주확인으로 전환한다 (출고지시 → 발주확인).
 * {@link ShipDirectMapper#update} 의 WHERE 절이 {@code OrderState = '출고지시'} 를 요구하므로
 * 이미 처리된 건이나 상태가 어긋난 건은 0건 갱신으로 자연히 걸러진다.
 *
 * <p>ShipDirectService(오케스트레이션, checkBatch)와 다른 빈으로 분리한 이유는 건별
 * {@code REQUIRES_NEW} 격리를 위해서다(출고지시의 ShipDirectIssueService 와 같은 패턴).</p>
 */
@Service
@RequiredArgsConstructor
public class ShipDirectCheckService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ShipDirectMapper shipDirectMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderLineLock lineLock;   // 상태 확인~갱신 사이에 다른 요청이 끼어들지 못하게 행 잠금

    /** 확인 시각을 지금으로 해서 1건 처리한다(한 건씩 확인하는 호출용). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String checkOne(Integer orderNo, Integer orderProdNo, Integer orderChangeNo, String registId, String registName) {
        return checkOne(orderNo, orderProdNo, orderChangeNo, registId, registName, LocalDateTime.now().format(DT));
    }

    /**
     * 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계).
     * shipCheckDate 는 한 번의 발주확인 묶음이 모두 같은 값을 쓴다 — 발주 목록(tShpShipCheck)의 헤더와 발주서 행을
     * 이 시각의 동등 비교로 잇기 때문이다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String checkOne(Integer orderNo, Integer orderProdNo, Integer orderChangeNo, String registId, String registName,
                           String shipCheckDate) {
        // 원본 라인을 잠그고 출고지시 상태인지 확인한다. 배송 테이블(tShpShipDirect)만 보면 이미 주문취소·환불된 라인도
        // 계속 진행시키고, 뒤따르는 무조건 UPDATE 가 주문취소 라인을 발주확인으로 되살린다.
        OrderProd line = lineLock.lockLine(orderNo, orderProdNo);
        if (line == null) return "주문라인을 찾을 수 없습니다.";
        if (!"출고지시".equals(line.getOrderState())) {
            return "출고지시 상태가 아니거나 이미 발주확인 처리된 건입니다. (현재 상태: " + line.getOrderState() + ")";
        }
        String now = shipCheckDate;

        Map<String, Object> upd = new HashMap<>();
        upd.put("orderNo", orderNo);
        upd.put("orderProdNo", orderProdNo);
        upd.put("orderChangeNo", orderChangeNo);
        upd.put("orderState", "발주확인");
        upd.put("changeId", registId);
        upd.put("changeName", registName);
        upd.put("changeDate", now);

        int updated = shipDirectMapper.update(upd);
        if (updated == 0) {
            return "출고지시 상태가 아니거나 이미 발주확인 처리된 건입니다.";
        }

        // 발주서(Print) 행도 같은 시각으로 확인 처리 — 발주서 행이 없는 옛 출고지시 건은 0행이라 그대로 지나간다.
        Map<String, Object> print = new HashMap<>();
        print.put("orderNo", orderNo);
        print.put("orderProdNo", orderProdNo);
        print.put("shipCheckDate", now);
        print.put("changeId", registId);
        print.put("changeName", registName);
        shipDirectMapper.confirmPrint(print);

        Map<String, Object> prodUpd = new HashMap<>();
        prodUpd.put("orderNo", orderNo);
        prodUpd.put("orderProdNo", orderProdNo);
        prodUpd.put("orderState", "발주확인");
        prodUpd.put("changeId", registId);
        prodUpd.put("changeName", registName);
        orderProdMapper.updateOrderState(prodUpd);

        return null;
    }
}
