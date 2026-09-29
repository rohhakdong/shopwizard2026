package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
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

    /** 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String checkOne(Integer orderNo, Integer orderProdNo, Integer orderChangeNo, String registId, String registName) {
        String now = LocalDateTime.now().format(DT);

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
