package com.shopwizard.operate.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.operate.model.ConsltInquiry;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.service.ConsltInquiryService;
import com.shopwizard.profile.service.CustInfoLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 고객문의 전체 조회 화면(operate-conslt)의 백엔드(읽기 전용). 상담에는 고객 연락처가 들어 있어 주문상세의 고객상담과 같은 정책 —
 * <b>상점 제한 계정은 볼 수 없고</b>, 채널·서비스 제한 계정은 자기 범위의 상담만(채널·서비스 조건은 {@code DataScopeFilter} 가 고정).
 * 조회할 때마다 개인정보 조회 기록을 남긴다(데이터를 읽은 뒤에만, 기록이 실패해도 조회는 막지 않는다).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/conslt")
public class ConsltInquiryController {

    private final ConsltInquiryService service;
    private final DataScopeGuard scopeGuard;
    private final CustInfoLogService logService;

    @GetMapping("/list")
    public ListPage<ConsltInquiry> list(@RequestParam Map<String, String> p, HttpServletRequest request) {
        if (scopeGuard.scope(request).shopCode() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 계정은 고객문의를 볼 수 없습니다.");
        }
        ListPage<ConsltInquiry> page = service.list(p, intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
        logAccess(request, p);
        return page;
    }

    private static final List<String> LOGGED = List.of("pStartDate", "pEndDate", "pCustConsltType", "pCustConsltReasonType", "pConsltMediaType",
            "pAnswrYn", "pChnlCode", "pOrderNo", "pCustName", "pPhone", "pKeyword", "pMngrName", "pPageOffset", "pPageSize");

    private void logAccess(HttpServletRequest request, Map<String, String> p) {
        try {
            StringJoiner sj = new StringJoiner(", ", "{", "}");
            for (String k : LOGGED) {
                String v = p.get(k);
                if (v != null && !v.isBlank()) sj.add(k + "=" + v);
            }
            AuthSession.Actor actor = AuthSession.actor(request);
            logService.record(request.getRequestURI(), sj.toString(), "custConsltService.list(map)", "consltInquiryMapper", "tOprCustConslt",
                    actor == null ? null : actor.loginId(), actor == null ? null : actor.name(), request.getRemoteAddr());
        } catch (RuntimeException e) {
            log.error("개인정보 조회 기록 실패 uri={}", request.getRequestURI(), e);
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
