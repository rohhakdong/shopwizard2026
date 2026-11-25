package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.service.LegacyUpriceMigrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 레거시 발주단가매칭 이관(단가매칭결과 화면의 "레거시 이관"). 전체 상점의 규칙을 한꺼번에 다루는 일이라 제한 없는 계정만 쓸 수 있다.
 * 미리보기는 읽기 전용이고, 확정은 미리보기에서 NEW 인 키만 저장한다(이미 있는 규칙은 덮어쓰지 않음).
 */
@RestController
@RequestMapping("/order/uprice-match/legacy-migrate")
@RequiredArgsConstructor
public class LegacyUpriceMigrationController {

    private final LegacyUpriceMigrationService service;
    private final DataScopeGuard scopeGuard;

    @PostMapping("/preview")
    public LegacyUpriceMigrationService.Preview preview(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        return service.preview(prefix(body));
    }

    @PostMapping("/commit")
    public LegacyUpriceMigrationService.CommitResult commit(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        AuthSession.Actor actor = AuthSession.actor(request);
        return service.commit(prefix(body), actor == null ? null : actor.name());
    }

    private static String prefix(Map<String, String> body) { return body == null ? null : body.get("keyPrefix"); }
}
