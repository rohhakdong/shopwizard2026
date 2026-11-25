package com.shopwizard.ship.web;

import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.ship.model.ShipPlaceRule;
import com.shopwizard.ship.service.DeliRstrctZipcodeService;
import com.shopwizard.ship.service.ShipPlaceRuleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 배송제한 규칙 화면(ship-place-rule). 목록의 서비스/상점 제한은 DataScopeFilter 가 GET 파라미터로 고정하고, 단건·쓰기는 규칙의 상점 단위로
 * {@link OrderMasterGuard} 가 검사한다. 감사값은 ActorAuditAdvice 가 세션 값으로 덮어쓴다.
 */
@RestController
@RequestMapping("/ship/place-rule")
@RequiredArgsConstructor
public class ShipPlaceRuleController {

    private final ShipPlaceRuleService service;
    private final DeliRstrctZipcodeService zipService;
    private final OrderMasterGuard guard;

    @GetMapping("/list")
    public List<ShipPlaceRule> selectList(@RequestParam Map<String, Object> params) { return service.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return service.selectCount(params); }

    @GetMapping
    public ShipPlaceRule select(@RequestParam("ruleNo") Integer ruleNo, HttpServletRequest request) {
        ShipPlaceRule row = service.find(ruleNo);
        if (row != null) guard.requireShop(request, row.getShopCode());
        return row;
    }

    /** 우편번호 구분에 등록된 우편번호 수 — 규칙 저장 화면의 경고("이 구분에는 아직 우편번호가 없습니다")용. */
    @GetMapping("/zip-count")
    public Map<String, Object> zipCount(@RequestParam("sect") String sect) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sect", sect);
        out.put("count", zipService.countBySect(sect));
        return out;
    }

    @PostMapping
    public ShipPlaceRule create(@RequestBody ShipPlaceRule body, HttpServletRequest request) {
        guard.requireShop(request, trim(body.getShopCode()));
        return service.create(body);
    }

    @PutMapping
    public ShipPlaceRule modify(@RequestBody ShipPlaceRule body, HttpServletRequest request) {
        ShipPlaceRule existing = service.find(body.getRuleNo());
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        guard.requireShop(request, trim(body.getShopCode()));   // 다른 상점으로 옮기는 경우
        return service.modify(body);
    }

    @DeleteMapping
    public void remove(@RequestParam("ruleNo") Integer ruleNo, HttpServletRequest request) {
        ShipPlaceRule existing = service.find(ruleNo);
        if (existing != null) guard.requireShop(request, existing.getShopCode());
        service.remove(ruleNo);
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
