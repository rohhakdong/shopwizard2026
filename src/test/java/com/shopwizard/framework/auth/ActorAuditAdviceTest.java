package com.shopwizard.framework.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 어드바이스는 "세션으로 인증된 요청"에서만, 요청 메서드에 맞춰 바디의 감사 필드를 덮어쓴다. */
class ActorAuditAdviceTest {

    private final ActorAuditAdvice advice = new ActorAuditAdvice();
    private final HttpInputMessage input = new MockHttpInputMessage(new byte[0]);

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    private MockHttpServletRequest bind(String method) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/catalog/brand");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        return req;
    }

    private Object read(Object body) {
        return advice.afterBodyRead(body, input, null, Map.class, null);
    }

    private static Map<String, Object> body() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("registId", "FORGED");
        m.put("changeId", "FORGED");
        return m;
    }

    @Test
    void 인증된_요청이면_바디의_감사_필드를_세션_값으로_덮어쓴다() {
        MockHttpServletRequest req = bind("POST");
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("loginId", "admin01", "name", "홍길동"));
        AuthSession.markActor(req, AuthSession.Type.MNGR);

        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) read(body());

        assertThat(out).containsEntry("registId", "admin01").containsEntry("registName", "홍길동")
                .containsEntry("changeId", "admin01");
    }

    @Test
    void 요청_메서드에_따라_PUT_은_수정자_계열을_채운다() {
        MockHttpServletRequest req = bind("PUT");
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("loginId", "admin01", "name", "홍길동"));
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        Map<String, Object> b = new LinkedHashMap<>();

        read(b);

        assertThat(b).containsEntry("changeId", "admin01").containsEntry("changeName", "홍길동")
                .doesNotContainKey("registId");
    }

    @Test
    void 인증을_거치지_않은_공개_요청의_바디는_건드리지_않는다() {
        bind("POST");   // actor 속성 없음 — 회원가입/결제 콜백 같은 공개 경로
        Map<String, Object> b = body();

        read(b);

        assertThat(b).containsEntry("registId", "FORGED").containsEntry("changeId", "FORGED");
    }

    @Test
    void 요청_컨텍스트가_없으면_바디를_그대로_돌려준다() {
        Map<String, Object> b = body();

        assertThat(read(b)).isSameAs(b);
        assertThat(b).containsEntry("registId", "FORGED");
    }

    @Test
    void 모든_RequestBody_에_적용된다() {
        assertThat(advice.supports(null, Map.class, null)).isTrue();
    }
}
