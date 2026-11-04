package com.shopwizard.profile.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.profile.model.CustDeliAddr;
import com.shopwizard.profile.service.CustDeliAddrService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 로그인한 회원 본인의 배송지 관리(shop.html 마이페이지 "배송지 관리", 주문서 배송지 선택).
 * CUST 인터셉터가 걸리는 경로이고({@code WebMvcConfig}), 관리자용 {@link CustDeliAddrController}(읽기 전용,
 * 관리자 인증) 와는 분리돼 있다.
 *
 * <p>custId 는 <b>항상 서버 세션</b>에서 꺼내고 요청의 custId 는 무시한다 — 다른 회원의 배송지를 보거나 고치는 것(IDOR)을
 * 막는다. 서비스의 모든 쿼리가 CustId 를 조건으로 걸어 소유권을 한 번 더 확인한다. 삭제도 {@code @RequestBody} 없이 경로로 받는다
 * (프런트 {@code Api.delete()} 는 본문을 보내지 않는다).</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/cust/me/deli-addrs")
public class CustDeliAddrMeController {

    private final CustDeliAddrService service;

    @GetMapping
    public List<CustDeliAddr> list(HttpServletRequest request) {
        return service.selectMine(custId(request));
    }

    @PostMapping
    public CustDeliAddr add(@RequestBody CustDeliAddr body, HttpServletRequest request) {
        Actor me = actor(request);
        return service.insertMine(custId(request), body, me.id(), me.name());
    }

    @PutMapping("/{deliAddrNo}")
    public CustDeliAddr update(@PathVariable Integer deliAddrNo, @RequestBody CustDeliAddr body, HttpServletRequest request) {
        Actor me = actor(request);
        return service.updateMine(custId(request), deliAddrNo, body, me.id(), me.name());
    }

    @PutMapping("/{deliAddrNo}/default")
    public Map<String, Object> setDefault(@PathVariable Integer deliAddrNo, HttpServletRequest request) {
        service.setDefaultMine(custId(request), deliAddrNo);
        return Map.of("success", true);
    }

    @DeleteMapping("/{deliAddrNo}")
    public Map<String, Object> delete(@PathVariable Integer deliAddrNo, HttpServletRequest request) {
        service.deleteMine(custId(request), deliAddrNo);
        return Map.of("success", true);
    }

    private static int custId(HttpServletRequest request) {
        Integer id = AuthSession.custId(request);
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return id;
    }

    private record Actor(String id, String name) {}

    private static Actor actor(HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.CUST);
        if (me == null) return new Actor(null, null);
        return new Actor(me.get("loginId"), me.get("name"));
    }
}
