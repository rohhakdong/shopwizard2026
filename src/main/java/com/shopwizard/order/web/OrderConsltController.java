package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.OrderConslt;
import com.shopwizard.order.model.OrderConsltRequest;
import com.shopwizard.order.service.OrderConsltService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 주문상세의 고객상담. 상담에는 고객 연락처가 들어 있어 상점 제한 계정(레거시도 상점 계정에게는 상담 열을 숨겼다)은 볼 수도 쓸 수도 없고,
 * 그 밖의 제한 계정은 자기 범위의 주문 상담만 다룬다({@link DataScopeGuard#requireOrder}). 처리 수정은 상담 행이 가리키는 주문으로 검사한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/order/conslt")
public class OrderConsltController {
    private final OrderConsltService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping
    public List<OrderConslt> list(@RequestParam("orderNo") Integer orderNo, HttpServletRequest request) {
        requireConsltAccess(request, orderNo);
        return service.list(orderNo);
    }

    @PostMapping
    public OrderConslt register(@RequestBody OrderConsltRequest req, HttpServletRequest request) {
        requireConsltAccess(request, req.getOrderNo());
        return service.register(req, AuthSession.actor(request));
    }

    @PutMapping
    public OrderConslt answer(@RequestBody OrderConsltRequest req, HttpServletRequest request) {
        requireNotShopScoped(request);   // 상담 행을 읽기 전에 먼저 거른다
        OrderConslt row = req.getCustConsltNo() == null ? null : service.find(req.getCustConsltNo());
        // 없는 상담이면 서비스가 400 으로 알리고, 있으면 그 상담이 가리키는 주문의 범위를 검사한다(주문번호가 숫자가 아니면 제한 계정은 거절).
        if (row != null) requireConsltAccess(request, toInt(row.getOrderNo()));
        else requireConsltAccess(request, null);
        return service.answer(req, AuthSession.actor(request));
    }

    private void requireNotShopScoped(HttpServletRequest request) {
        if (scopeGuard.scope(request).shopCode() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 계정은 고객상담을 볼 수 없습니다.");
        }
    }

    private void requireConsltAccess(HttpServletRequest request, Integer orderNo) {
        requireNotShopScoped(request);
        scopeGuard.requireOrder(request, orderNo);
    }

    private static Integer toInt(String s) {
        try { return s == null ? null : Integer.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
