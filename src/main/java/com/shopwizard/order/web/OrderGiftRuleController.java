package com.shopwizard.order.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.model.OrderGiftRule;
import com.shopwizard.order.service.OrderGiftRuleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발주 사은품 규칙 화면(order-gift-rule). 목록의 서비스/상점 제한은 DataScopeFilter 가 GET 파라미터로 고정하고, 단건·쓰기는 규칙의 상점 단위로
 * {@link OrderMasterGuard} 가 검사한다(채널을 지정하면 서비스 제한 계정은 자기 서비스 채널만). 감사값은 ActorAuditAdvice 가 세션 값으로 덮어쓴다.
 */
@RestController
@RequestMapping("/order/gift-rule")
@RequiredArgsConstructor
public class OrderGiftRuleController {
    private final OrderGiftRuleService service;
    private final OrderMasterGuard guard;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public List<OrderGiftRule> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public OrderGiftRule select(@RequestParam("ruleNo") Integer ruleNo, HttpServletRequest request) {
        OrderGiftRule row = service.find(ruleNo);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    @PostMapping
    public OrderGiftRule create(@RequestBody OrderGiftRule body, HttpServletRequest request) {
        checkTarget(body, request);
        return service.create(body);
    }

    @PutMapping
    public OrderGiftRule modify(@RequestBody OrderGiftRule body, HttpServletRequest request) {
        OrderGiftRule existing = service.find(body.getRuleNo());
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        checkTarget(body, request);   // 다른 상점·채널로 옮기는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("ruleNo") Integer ruleNo, HttpServletRequest request) {
        OrderGiftRule existing = service.find(ruleNo);
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        service.remove(ruleNo);
    }

    /** 요청 본문이 가리키는 상점·채널이 내 범위인가. */
    private void checkTarget(OrderGiftRule body, HttpServletRequest request) {
        guard.requireShop(request, trim(body.getShopCode()));
        String chnl = trim(body.getChnlCode());
        if (chnl != null) scopeGuard.requireChnlSvc(request, chnl);
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
