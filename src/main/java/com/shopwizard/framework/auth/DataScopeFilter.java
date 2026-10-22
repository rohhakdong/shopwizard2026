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
 * 상점/채널/서비스로 제한된 관리자(DataScope)의 조회 요청에서 {@code pShopCode}/{@code pChnlCode}/{@code pSvcCode} 를 세션 값으로 고정한다.
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
            // 스토어프론트 공개 조회 경로(/product/prod/list 등)도 관리자 세션이 있으면 같은 범위로 거른다 — 판매 상품 관리 화면이
            // 같은 경로를 쓰기 때문이다. 관리자 세션이 없는 고객·비회원은 DataScope.of 가 제한 없음을 돌려줘 영향이 없다.
            if (MngrPaths.isMngrProtected(path)) {
                DataScope scope = DataScope.of(request);
                if (scope.anyScoped()) {
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
            // 서비스 제한: 클라이언트가 보낸 pSvcCode 는 항상 세션 서비스로 덮어쓴다(비어 있어도 채운다 — 주문 목록 등은 pSvcCode 가
            // 없으면 아예 동작하지 않는다). svcCode 이름으로 서비스를 거르는 조회가 있으면 그것도 내 서비스로 좁힌다.
            if (scope.svcCode() != null) {
                merged.put("pSvcCode", new String[]{scope.svcCode()});
                if (merged.containsKey("svcCode")) merged.put("svcCode", new String[]{scope.svcCode()});
            }
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
