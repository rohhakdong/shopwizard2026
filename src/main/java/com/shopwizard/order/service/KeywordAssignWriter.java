package com.shopwizard.order.service;

import com.shopwizard.order.mapper.KeywordTargetMapper;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드로 가져온 라인 1건을 상점에 배정한다. 건별 {@code REQUIRES_NEW} — 한 건이 실패해도 나머지는 그대로 반영된다.
 * 라인을 잠근 뒤 아직 지불완료이고 상점이 비어 있을 때만 쓴다(먼저 실행한 다른 상점이나 출고지시·취소가 끼어든 라인은 건너뜀).
 */
@Service
@RequiredArgsConstructor
public class KeywordAssignWriter {

    private final OrderLineLock lineLock;
    private final KeywordTargetMapper mapper;

    /** @return 배정했으면 true, 그 사이 바뀌어 건너뛰었으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean assign(Integer orderNo, Integer orderProdNo, String shopCode, String shopName, String actorId, String actorName) {
        OrderProd op = lineLock.lockLine(orderNo, orderProdNo);
        if (op == null || !"지불완료".equals(op.getOrderState())) return false;
        if (op.getShopCode() != null && !op.getShopCode().isBlank()) return false;
        return mapper.assignShop(orderNo, orderProdNo, shopCode, shopName, actorId, actorName) > 0;
    }
}
