package com.shopwizard.company.web;

import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.company.service.SupplyCompNoticeService;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.NoticeRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 공급사 공지 관리 화면(operate-supply-notice)의 백엔드. 채널·회사 구분 없이 모두에게 보이는 전역 공지라
 * 조회는 화면 권한만, <b>쓰기(등록·수정·삭제)는 상점·채널·서비스 제한이 없는 계정만</b> 한다.
 */
@RestController
@RequestMapping("/company/supply-comp-notice")
@RequiredArgsConstructor
public class SupplyCompNoticeController {
    private final SupplyCompNoticeService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public ListPage<SupplyCompNotice> list(@RequestParam Map<String, String> p) {
        return service.list(p.get("pTitle"), p.get("pStatus"), p.get("pStartDate"), p.get("pEndDate"),
                intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
    }

    @GetMapping("/{noticeNo}")
    public SupplyCompNotice get(@PathVariable Integer noticeNo) {
        return service.get(noticeNo);
    }

    @PostMapping
    public SupplyCompNotice create(@RequestBody NoticeRequest req, HttpServletRequest request) {
        requireWriter(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.create(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @PutMapping
    public SupplyCompNotice update(@RequestBody NoticeRequest req, HttpServletRequest request) {
        requireWriter(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.update(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @DeleteMapping("/{noticeNo}")
    public Map<String, Object> delete(@PathVariable Integer noticeNo, HttpServletRequest request) {
        requireWriter(request);
        service.delete(noticeNo);
        return Map.of("deleted", noticeNo);
    }

    /** 전역 공지라 범위가 없다 — 제한 없는 계정(상점·채널·서비스 모두 미지정)만 쓴다. */
    private void requireWriter(HttpServletRequest request) {
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
