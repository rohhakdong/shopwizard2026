package com.shopwizard.operate.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.Notice;
import com.shopwizard.operate.model.NoticeRequest;
import com.shopwizard.operate.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 공지사항(쇼핑몰 공지) 관리 화면(operate-notice)의 백엔드. 공지는 채널에 속한다 — 채널 제한 계정은 자기 채널 공지만,
 * 서비스 제한 계정은 자기 서비스의 채널 공지만 다루고, 상점 제한 계정은 쓸 수 없다. 목록의 채널·서비스 조건은
 * {@code DataScopeFilter} 가 GET 파라미터(pChnlCode/pSvcCode)를 세션 값으로 고정한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/notice")
public class NoticeController {
    private final NoticeService service;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public ListPage<Notice> list(@RequestParam Map<String, String> p, HttpServletRequest request) {
        requireNotShopScoped(request);
        return service.list(p.get("pChnlCode"), p.get("pSvcCode"), p.get("pNoticeTitle"), p.get("pNoticeCntnts"), p.get("pRegistName"),
                p.get("pStatus"), p.get("pStartDate"), p.get("pEndDate"), intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
    }

    @GetMapping("/{noticeNo}")
    public Notice get(@PathVariable Integer noticeNo, HttpServletRequest request) {
        requireNotShopScoped(request);
        Notice n = service.get(noticeNo);
        scopeGuard.requireRow(request, n, null, n.getChnlCode());
        return n;
    }

    @PostMapping
    public Notice create(@RequestBody NoticeRequest req, HttpServletRequest request) {
        requireWritable(request, req);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.create(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @PutMapping
    public Notice update(@RequestBody NoticeRequest req, HttpServletRequest request) {
        requireNotShopScoped(request);
        Notice current = service.get(req.getNoticeNo());
        scopeGuard.requireRow(request, current, null, current.getChnlCode());   // 남의 채널 공지는 못 고친다
        requireWritable(request, req);                                          // 옮겨 갈 채널도 내 범위여야 한다
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.update(req, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    @DeleteMapping("/{noticeNo}")
    public Map<String, Object> delete(@PathVariable Integer noticeNo, HttpServletRequest request) {
        requireNotShopScoped(request);
        Notice current = service.get(noticeNo);
        scopeGuard.requireRow(request, current, null, current.getChnlCode());
        service.delete(noticeNo);
        return Map.of("deleted", noticeNo);
    }

    /** 쓰기 범위: 상점 제한 불가, 채널 제한 계정은 채널을 자기 채널로 고정, 서비스 제한 계정은 채널이 내 서비스 것이어야 한다. */
    private void requireWritable(HttpServletRequest request, NoticeRequest req) {
        requireNotShopScoped(request);
        DataScope scope = scopeGuard.scope(request);
        if (scope.chnlCode() != null) req.setChnlCode(scope.chnlCode());
        scopeGuard.requireChnlSvc(request, req.getChnlCode());
    }

    private void requireNotShopScoped(HttpServletRequest request) {
        if (scopeGuard.scope(request).shopCode() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 계정은 공지를 다룰 수 없습니다.");
        }
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
