package com.shopwizard.ship.web;

import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.ship.model.ShipCheck;
import com.shopwizard.ship.service.ShipCheckService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 발주 목록(tShpShipCheck) 조회. 발주확인 한 묶음당 상점별 1건이 쌓인다(ShipDirectService.checkBatch).
 * 목록·건수는 DataScopeFilter 가 상점/서비스 제한 계정의 pShopCode/pSvcCode 를 고정하고, 채널 제한 계정은 쓰지 못한다.
 */
@RestController
@RequestMapping("/ship/ship-check")
@RequiredArgsConstructor
public class ShipCheckController {
    private final ShipCheckService shipCheckService;
    private final OrderMasterGuard guard;

    @GetMapping("/list")
    public List<ShipCheck> selectList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        guard.requireShop(request, shopOf(params));
        return shipCheckService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        guard.requireShop(request, shopOf(params));
        return shipCheckService.selectCount(params);
    }

    @GetMapping
    public ShipCheck select(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        ShipCheck row = shipCheckService.select(params);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    private static String shopOf(Map<String, Object> params) {
        Object v = params.get("pShopCode");
        return v == null ? null : String.valueOf(v);
    }
}
