package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.ManualOrderRequest;
import com.shopwizard.order.model.ManualOrderResult;
import com.shopwizard.order.service.ManualOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주문관리 화면의 "주문등록" — 관리자가 직접 입력한 주문(지불완료·수기 결제). 감사값(registId/registName)은 세션 값으로 덮어써진다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/order/manual")
public class ManualOrderController {
    private final ManualOrderService manualOrderService;
    private final DataScopeGuard scopeGuard;

    @PostMapping
    public ManualOrderResult create(@RequestBody ManualOrderRequest req, HttpServletRequest request) {
        // 채널 제한 계정은 body 의 채널을 믿지 않고 항상 자기 채널로 등록한다.
        DataScope scope = scopeGuard.scope(request);
        if (scope.chnlCode() != null) req.setChnlCode(scope.chnlCode());
        // 서비스 제한 계정은 다른 서비스의 채널로 등록할 수 없고, 상점 제한 계정은 자기 상점 상품만 담을 수 있다.
        scopeGuard.requireChnlSvc(request, req.getChnlCode());
        if (req.getLines() != null) {
            for (ManualOrderRequest.Line line : req.getLines()) {
                scopeGuard.requirePrdProd(request, line == null ? null : line.getProdCode());
            }
        }
        return manualOrderService.create(req);
    }
}
