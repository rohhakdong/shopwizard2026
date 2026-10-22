package com.shopwizard.framework.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 로그인한 관리자가 볼 수 있는 데이터 범위. 세션(MNGR claims)의 shopCode/chnlCode 가 비어 있지 않으면 그 값으로
 * 제한하고(둘 다 있으면 AND), 비어 있으면 무제한이다. 클라이언트가 보낸 값은 쓰지 않는다.
 *
 * <p>서비스(svcCode)는 테넌트 경계다 — 관리자는 자기 역할의 서비스 데이터만 본다. 예외는 <b>전체 권한 계정</b>
 * (역할·메뉴·기능 관리를 모두 쓸 수 있는 시스템 관리자, 로그인 시 claims 의 {@code svcAll=Y}) 뿐이다. 서비스가 비어 있는 역할이나
 * {@code svcAll} 이 없는 기존 세션은 fail-closed — 서비스 코드가 없으면 아무 서비스도 못 본다({@link #NO_SERVICE}).</p>
 *
 * <p>{@link #scoped()} 는 상점·채널 제한만 뜻한다(정산 생성·마감처럼 여러 상점에 걸친 쓰기를 막는 기준이라 서비스 제한만으로는
 * 막히면 안 된다). 서비스 제한은 {@link #svcScoped()} 로 따로 본다.</p>
 */
public record DataScope(String shopCode, String chnlCode, String svcCode) {

    /** 서비스 코드가 없는 계정이 쓰는 값 — 어떤 서비스와도 일치하지 않는다(fail-closed). */
    public static final String NO_SERVICE = "~none~";

    public static final DataScope UNRESTRICTED = new DataScope(null, null, null);

    /** 상점/채널로 제한된 계정인가. */
    public boolean scoped() {
        return shopCode != null || chnlCode != null;
    }

    /** 서비스로 제한된 계정인가(전체 권한 계정은 아님). */
    public boolean svcScoped() {
        return svcCode != null;
    }

    /** 상점/채널/서비스 중 하나라도 제한이 걸려 있는가. */
    public boolean anyScoped() {
        return scoped() || svcScoped();
    }

    public static DataScope of(HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.MNGR);
        return me == null ? UNRESTRICTED : of(me);
    }

    public static DataScope of(Map<String, String> claims) {
        String svc = "Y".equals(claims.get("svcAll")) ? null
                : (blankToNull(claims.get("svcCode")) != null ? blankToNull(claims.get("svcCode")) : NO_SERVICE);
        return new DataScope(blankToNull(claims.get("shopCode")), blankToNull(claims.get("chnlCode")), svc);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
