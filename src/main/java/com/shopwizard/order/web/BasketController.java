package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.model.Basket;
import com.shopwizard.order.service.BasketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 고객(스토어프론트) 장바구니. 이 경로는 CUST 인터셉터로 보호되며, 어떤 고객의 장바구니인지는
 * 요청 파라미터가 아니라 서버 세션의 로그인 정보로만 결정한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/basket")
public class BasketController {
    private final BasketService basketService;

    @GetMapping("/list")
    public List<Basket> selectList(HttpServletRequest request) {
        return basketService.selectList(custId(request));
    }

    @GetMapping
    public Basket select(@RequestParam Integer basketNo, HttpServletRequest request) {
        return basketService.select(custId(request), basketNo);
    }

    @GetMapping("/item")
    public Basket selectByItem(@RequestParam String prodCode,
                               @RequestParam(required = false) String itemCode, HttpServletRequest request) {
        return basketService.selectByItem(custId(request), prodCode, itemCode);
    }

    @PostMapping
    public Basket insert(@RequestBody Basket basket, HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.CUST);
        return basketService.add(custId(request), me.get("loginId"), me.get("name"), basket);
    }

    @PutMapping
    public void update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.CUST);
        basketService.changeQty(custId(request), toInt(params.get("basketNo")), toInt(params.get("prodQty")),
                me.get("loginId"), me.get("name"));
    }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        basketService.delete(custId(request), toInt(params.get("basketNo")));
    }

    @DeleteMapping("/multi")
    public void deleteMulti(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Object arr = params.get("pBasketNoArr");
        List<Integer> nos = arr instanceof List<?> l ? l.stream().map(BasketController::toInt).toList() : List.of();
        basketService.deleteMulti(custId(request), nos);
    }

    @DeleteMapping("/clean")
    public void clean(HttpServletRequest request) {
        basketService.clean(custId(request));
    }

    private static String custId(HttpServletRequest request) {
        Integer id = AuthSession.custId(request);
        if (id == null || id == 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
        return id.toString();
    }

    private static int toInt(Object v) {
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다");
        }
    }
}
