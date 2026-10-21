package com.shopwizard.framework.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 상점/채널로 제한된 관리자(DataScope)의 조회 요청에서 {@code pShopCode}/{@code pChnlCode} 를 세션 값으로 고정한다.
 * 대부분의 목록·건수·집계 쿼리가 이 파라미터를 이미 지원하므로, 컨트롤러를 건드리지 않고 한 곳에서 "남의 업체 행"을
 * 걸러낸다. 클라이언트가 다른 값을 보내도 덮어쓴다(채널이 세션에 없으면 클라이언트가 보낸 pChnlCode 는 그대로 — 좁히는
 * 방향은 허용).
 *
 * <p>관리자 보호 경로의 GET/HEAD 만 대상이다 — 고객·상점·창고용 경로, 스토어프론트 공개 조회는 건드리지 않는다
 * ({@link MngrPaths}). 쓰기와 단건 조회는 {@link DataScopeGuard} 가 행 소유권으로 막는다.</p>
 */
public class DataScopeFilter extends OncePerRequestFilter {

    private final boolean enabled;

    public DataScopeFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (enabled && isRead(request)) {
            String path = path(request);
            if (MngrPaths.isMngrProtected(path) && !MngrPaths.isPublicRead(path)) {
                DataScope scope = DataScope.of(request);
                if (scope.scoped()) {
                    chain.doFilter(new ScopedRequest(request, scope), response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isRead(HttpServletRequest request) {
        String m = request.getMethod();
        return "GET".equalsIgnoreCase(m) || "HEAD".equalsIgnoreCase(m);
    }

    private static String path(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) path = path.substring(context.length());
        return path;
    }

    /** 파라미터 조회 4개 메서드를 모두 덮어써서 어떤 경로로 읽어도 같은 값이 나오게 한다. */
    static final class ScopedRequest extends HttpServletRequestWrapper {
        private final Map<String, String[]> params;

        ScopedRequest(HttpServletRequest request, DataScope scope) {
            super(request);
            Map<String, String[]> merged = new LinkedHashMap<>(request.getParameterMap());
            if (scope.shopCode() != null) merged.put("pShopCode", new String[]{scope.shopCode()});
            if (scope.chnlCode() != null) merged.put("pChnlCode", new String[]{scope.chnlCode()});
            this.params = Collections.unmodifiableMap(merged);
        }

        @Override public String getParameter(String name) {
            String[] v = params.get(name);
            return v == null || v.length == 0 ? null : v[0];
        }

        @Override public String[] getParameterValues(String name) {
            return params.get(name);
        }

        @Override public Map<String, String[]> getParameterMap() {
            return params;
        }

        @Override public Enumeration<String> getParameterNames() {
            return Collections.enumeration(params.keySet());
        }
    }
}
