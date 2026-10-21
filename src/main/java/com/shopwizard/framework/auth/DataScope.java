package com.shopwizard.framework.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 로그인한 관리자가 볼 수 있는 데이터 범위. 세션(MNGR claims)의 shopCode/chnlCode 가 비어 있지 않으면 그 값으로
 * 제한하고(둘 다 있으면 AND), 비어 있으면 무제한이다. 클라이언트가 보낸 값은 쓰지 않는다.
 */
public record DataScope(String shopCode, String chnlCode) {

    public static final DataScope UNRESTRICTED = new DataScope(null, null);

    public boolean scoped() {
        return shopCode != null || chnlCode != null;
    }

    public static DataScope of(HttpServletRequest request) {
        Map<String, String> me = AuthSession.get(request, AuthSession.Type.MNGR);
        return me == null ? UNRESTRICTED : of(me);
    }

    public static DataScope of(Map<String, String> claims) {
        return new DataScope(blankToNull(claims.get("shopCode")), blankToNull(claims.get("chnlCode")));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
