package com.shopwizard.framework.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

/**
 * 로그인 상태를 서버 세션(HttpSession)에 보관한다. 예전에는 로그인 상태를 "클라이언트가 보낸 쿠키 값"으로
 * 판단해서 Cookie 헤더만 위조하면 누구나 관리자/고객/상점/창고 계정을 사칭할 수 있었다. 이제 인증 여부와
 * 계정 정보(claims)는 전부 서버가 세션에 저장한 값만 믿는다.
 *
 * <p>관리자·고객·상점·창고는 서로 독립된 세션 속성 4개로 보관한다 — 예전의 쿠키 4세트와 같은 격리
 * 성질을 유지해서, 상점 계정으로 로그인해도 관리자 API에는 접근할 수 없고, 관리자가 같은 브라우저에서
 * 고객으로 따로 로그인해 스토어프론트를 테스트할 수도 있다.</p>
 */
public final class AuthSession {

    public enum Type {
        MNGR("auth.mngr"), CUST("auth.cust"), SHOP("auth.shop"), WAREHS("auth.warehs");

        private final String attribute;

        Type(String attribute) { this.attribute = attribute; }
    }

    /**
     * 이 요청을 인증시킨 주체(관리자/고객/상점/창고)의 로그인 정보. 인터셉터가 세션 인증에 성공했을 때만 요청 속성에
     * 실어두며, 요청 바디의 감사 필드(registId/changeId …)를 이 값으로 덮어쓰는 데 쓴다({@link AuditFields}).
     */
    public record Actor(String loginId, String name) {}

    /** {@link #actor(HttpServletRequest)} 가 읽는 요청 속성 이름. */
    public static final String ACTOR_ATTR = "auth.actor";

    private AuthSession() {}

    /** 세션 인증을 통과한 요청에 행위자를 표시한다. 세션에 해당 주체가 없으면 아무것도 하지 않는다. */
    public static void markActor(HttpServletRequest request, Type type) {
        Map<String, String> claims = get(request, type);
        if (claims == null) return;
        request.setAttribute(ACTOR_ATTR, new Actor(claims.get("loginId"), claims.get("name")));
    }

    /** 이 요청을 세션으로 인증시킨 행위자. 공개/비인증 요청이면 null. */
    public static Actor actor(HttpServletRequest request) {
        return request.getAttribute(ACTOR_ATTR) instanceof Actor a ? a : null;
    }

    /** 로그인 성공 처리. 세션 고정 공격을 막기 위해 기존 세션이 있으면 세션 ID를 새로 발급한다. */
    public static void login(HttpServletRequest request, Type type, Map<String, String> claims) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        Map<String, String> safe = new HashMap<>();
        claims.forEach((k, v) -> safe.put(k, v == null ? "" : v));
        request.getSession(true).setAttribute(type.attribute, Map.copyOf(safe));
    }

    /** 해당 주체의 로그인만 해제한다. 다른 주체의 로그인이 남아있지 않으면 세션 자체를 폐기한다. */
    public static void logout(HttpServletRequest request, Type type) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        session.removeAttribute(type.attribute);
        for (Type t : Type.values()) {
            if (session.getAttribute(t.attribute) != null) return;
        }
        session.invalidate();
    }

    /** 로그인되어 있으면 서버가 저장해둔 계정 정보(claims), 아니면 null. */
    @SuppressWarnings("unchecked")
    public static Map<String, String> get(HttpServletRequest request, Type type) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute(type.attribute);
        return value instanceof Map ? (Map<String, String>) value : null;
    }

    public static boolean isLoggedIn(HttpServletRequest request, Type type) {
        return get(request, type) != null;
    }

    /** 로그인한 고객의 custId. 비회원(미로그인)이면 null. */
    public static Integer custId(HttpServletRequest request) {
        Map<String, String> claims = get(request, Type.CUST);
        if (claims == null) return null;
        try {
            return Integer.valueOf(claims.get("custId"));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
