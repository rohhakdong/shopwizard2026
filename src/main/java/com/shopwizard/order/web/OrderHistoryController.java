package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.mapper.OrderHistoryMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 주문상세의 배송·반품·결제 내역(읽기 전용). 주문 헤더와 같은 범위 검사({@link DataScopeGuard#requireOrder})를 한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/order/history")
public class OrderHistoryController {
    private final OrderHistoryMapper mapper;
    private final DataScopeGuard scopeGuard;

    /** {ships: 출고 사본 전체, returns: 반품 사본 전체, pays: 결제} */
    @GetMapping
    public Map<String, Object> history(@RequestParam("orderNo") Integer orderNo, HttpServletRequest request) {
        scopeGuard.requireOrder(request, orderNo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ships", mapper.selectShips(orderNo));
        result.put("returns", mapper.selectReturns(orderNo));
        result.put("pays", mapper.selectPays(orderNo));
        return result;
    }
}
