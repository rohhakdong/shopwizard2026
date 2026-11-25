package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderProdCode;
import com.shopwizard.order.service.OrderProdCodeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발주상품코드 화면(order-prod-code). 목록의 서비스/상점 제한은 DataScopeFilter 가 GET 파라미터로 고정하고,
 * 단건·쓰기는 상점 단위로 {@link OrderMasterGuard} 가 검사한다. 감사값은 ActorAuditAdvice 가 세션 값으로 덮어쓴다.
 */
@RestController
@RequestMapping("/order/prod-code")
@RequiredArgsConstructor
public class OrderProdCodeController {
    private final OrderProdCodeService service;
    private final OrderMasterGuard guard;

    @GetMapping("/list")
    public List<OrderProdCode> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public OrderProdCode select(@RequestParam("matchProdCode") String matchProdCode, HttpServletRequest request) {
        OrderProdCode row = service.find(matchProdCode);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    @PostMapping
    public OrderProdCode create(@RequestBody OrderProdCode body, HttpServletRequest request) {
        if (notBlank(body.getShopCode())) guard.requireShop(request, body.getShopCode().trim());
        return service.create(body);
    }

    @PutMapping
    public OrderProdCode modify(@RequestBody OrderProdCode body, HttpServletRequest request) {
        OrderProdCode existing = service.find(body.getMatchProdCode());
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        if (notBlank(body.getShopCode())) guard.requireShop(request, body.getShopCode().trim());   // 다른 상점으로 옮기는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("matchProdCode") String matchProdCode, HttpServletRequest request) {
        OrderProdCode existing = service.find(matchProdCode);
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        service.remove(matchProdCode);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
