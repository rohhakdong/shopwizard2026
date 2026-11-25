package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderKeywrd;
import com.shopwizard.order.service.OrderKeywrdService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발주 상품명 키워드 화면(order-keywrd). 목록의 서비스/상점 제한은 DataScopeFilter 가 GET 파라미터로 고정하고, 단건·쓰기는 키워드의 상점 단위로
 * {@link OrderMasterGuard} 가 검사한다. 감사값은 ActorAuditAdvice 가 세션 값으로 덮어쓴다.
 */
@RestController
@RequestMapping("/order/keywrd")
@RequiredArgsConstructor
public class OrderKeywrdController {
    private final OrderKeywrdService service;
    private final OrderMasterGuard guard;

    @GetMapping("/list")
    public List<OrderKeywrd> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public OrderKeywrd select(@RequestParam("keywrdSeq") Integer keywrdSeq, HttpServletRequest request) {
        OrderKeywrd row = service.find(keywrdSeq);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    @PostMapping
    public OrderKeywrd create(@RequestBody OrderKeywrd body, HttpServletRequest request) {
        guard.requireShop(request, trim(body.getShopCode()));
        return service.create(body);
    }

    @PutMapping
    public OrderKeywrd modify(@RequestBody OrderKeywrd body, HttpServletRequest request) {
        OrderKeywrd existing = service.find(body.getKeywrdSeq());
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        guard.requireShop(request, trim(body.getShopCode()));   // 다른 상점으로 옮기는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("keywrdSeq") Integer keywrdSeq, HttpServletRequest request) {
        OrderKeywrd existing = service.find(keywrdSeq);
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        service.remove(keywrdSeq);
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
