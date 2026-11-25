package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderUprice;
import com.shopwizard.order.service.OrderUpriceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발주단가표(order-uprice) 와 발주단가매칭 화면의 단가코드 선택·새 코드 등록(order-uprice-match)이 함께 쓴다. 목록의 서비스/상점 제한은
 * DataScopeFilter 가 GET 파라미터로 고정하고, 단건·쓰기는 상점 단위로 {@link OrderMasterGuard} 가 검사한다(상점이 비면 공통 코드).
 */
@RestController
@RequestMapping("/order/uprice")
@RequiredArgsConstructor
public class OrderUpriceController {
    private final OrderUpriceService service;
    private final OrderMasterGuard guard;

    @GetMapping("/list")
    public List<OrderUprice> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public OrderUprice select(@RequestParam("matchUpriceCode") String matchUpriceCode, HttpServletRequest request) {
        OrderUprice row = service.find(matchUpriceCode);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    @PostMapping
    public OrderUprice create(@RequestBody OrderUprice body, HttpServletRequest request) {
        guard.requireShop(request, body.getShopCode() == null ? null : body.getShopCode().trim());
        return service.create(body);
    }

    @PutMapping
    public OrderUprice modify(@RequestBody OrderUprice body, HttpServletRequest request) {
        OrderUprice existing = service.find(body.getMatchUpriceCode());
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        guard.requireShop(request, body.getShopCode() == null ? null : body.getShopCode().trim());   // 다른 상점으로 옮기는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("matchUpriceCode") String matchUpriceCode, HttpServletRequest request) {
        OrderUprice existing = service.find(matchUpriceCode);
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        service.remove(matchUpriceCode);
    }
}
