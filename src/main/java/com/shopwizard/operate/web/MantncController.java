package com.shopwizard.operate.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.Mantnc;
import com.shopwizard.operate.model.MantncRequest;
import com.shopwizard.operate.service.MantncService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 유지보수 화면(operate-mantnc)의 백엔드. 요청은 상점 계정을 뺀 누구나 올리고, 채널·서비스 제한 계정은 <b>자기 요청만</b> 보고
 * 고치고 지운다. <b>작업 예정일 입력·작업완료 처리와 전체 요청 조회는 상점·채널·서비스 제한이 없는 계정만</b> 한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/mantnc")
public class MantncController {

    private final MantncService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public ListPage<Mantnc> list(@RequestParam Map<String, String> p, HttpServletRequest request) {
        String owner = ownerId(request);
        return service.list(owner, p.get("pStatus"), p.get("pKeyword"), p.get("pRequester"), p.get("pStartDate"), p.get("pEndDate"),
                intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(HttpServletRequest request) {
        String owner = ownerId(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        Map<String, Object> out = new java.util.LinkedHashMap<>(service.summary(owner));
        out.put("canHandle", owner == null);   // 화면이 처리 버튼(예정일·작업완료)을 보일지 판단 — 서버가 최종 검사한다
        out.put("loginId", actor == null ? "" : actor.loginId());
        return out;
    }

    @GetMapping("/{mantncNo}")
    public Mantnc get(@PathVariable Integer mantncNo, HttpServletRequest request) {
        return service.get(mantncNo, ownerId(request));
    }

    @PostMapping
    public Mantnc create(@RequestBody MantncRequest req, HttpServletRequest request) {
        ownerId(request);   // 상점 제한 계정 거절
        AuthSession.Actor actor = AuthSession.actor(request);
        Map<String, String> claims = AuthSession.get(request, AuthSession.Type.MNGR);
        return service.create(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name(), claims == null ? null : claims.get("roleUid"));
    }

    @PutMapping
    public Mantnc update(@RequestBody MantncRequest req, HttpServletRequest request) {
        String owner = ownerId(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.update(req, owner, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @DeleteMapping("/{mantncNo}")
    public Map<String, Object> delete(@PathVariable Integer mantncNo, HttpServletRequest request) {
        service.delete(mantncNo, ownerId(request));
        return Map.of("deleted", mantncNo);
    }

    @PutMapping("/schedule")
    public Mantnc schedule(@RequestBody MantncRequest req, HttpServletRequest request) {
        requireHandler(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.schedule(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @PutMapping("/complete")
    public Mantnc complete(@RequestBody MantncRequest req, HttpServletRequest request) {
        requireHandler(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.complete(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    /**
     * 이 요청의 "내 것" 범위: 상점 제한 계정은 거절(403), 채널·서비스 제한 계정은 자기 로그인 ID(자기 요청만),
     * 제한 없는 계정은 null(전체).
     */
    private String ownerId(HttpServletRequest request) {
        var scope = scopeGuard.scope(request);
        if (scope.shopCode() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 계정은 유지보수를 이용할 수 없습니다.");
        if (!scope.anyScoped()) return null;
        AuthSession.Actor actor = AuthSession.actor(request);
        if (actor == null || actor.loginId() == null || actor.loginId().isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인 정보를 확인할 수 없습니다.");
        return actor.loginId();
    }

    /** 작업 예정일·완료 처리는 제한 없는 계정만. */
    private void requireHandler(HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        scopeGuard.requireAllServices(request);
    }

    private static Integer intOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 값이 올바르지 않습니다.");
        }
    }
}
