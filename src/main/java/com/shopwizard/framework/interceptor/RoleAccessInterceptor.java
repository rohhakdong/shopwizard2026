package com.shopwizard.framework.interceptor;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.RoleAccessPolicy;
import com.shopwizard.framework.auth.RolePermissionService;
import com.shopwizard.framework.auth.RolePermissions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * 관리자(MNGR) 세션의 역할이 이 API 를 쓸 수 있는지 검사한다(403). {@link AuthInterceptor} 뒤에 같은 경로 패턴으로
 * 등록한다. 판정 규칙은 {@link RoleAccessPolicy} 참고.
 *
 * <p>mode: enforce=차단, log=차단하지 않고 거부될 요청만 WARN 으로 남김(단계적 적용용), off=검사 안 함.
 * 화면이 소유하지 않은 경로(unowned)는 모드와 무관하게 INFO 로 남겨, 화면 코드와 소유표의 어긋남을 찾는 데 쓴다.</p>
 */
public class RoleAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleAccessInterceptor.class);

    private final RolePermissionService permissions;
    private final String mode;
    private final List<String> publicReadPatterns;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RoleAccessInterceptor(RolePermissionService permissions, String mode, String... publicReadPatterns) {
        this.permissions = permissions;
        this.mode = mode == null ? "enforce" : mode.trim().toLowerCase();
        this.publicReadPatterns = List.of(publicReadPatterns);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("off".equals(mode)) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = path(request);
        String method = request.getMethod();
        // 스토어프론트 공개 조회는 관리자 세션이 있어도 역할과 무관하다(AuthInterceptor 와 같은 규칙).
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (read && publicReadPatterns.stream().anyMatch(p -> matcher.match(p, path))) return true;

        Map<String, String> me = AuthSession.get(request, AuthSession.Type.MNGR);
        if (me == null) return true;   // 관리자 로그인이 아니면 AuthInterceptor 의 몫

        RolePermissions perms = permissions.get(me.get("roleUid"));
        RoleAccessPolicy.Decision d = RoleAccessPolicy.decide(method, path, perms);
        if (d.unowned()) log.info("authz unowned {} {} -> {} ({})", method, path, d.result(), d.reason());
        if (d.allowed()) return true;

        log.warn("authz deny {} {} login={} role={} ({}){}", method, path, me.get("loginId"), me.get("roleUid"),
                d.reason(), "log".equals(mode) ? " [log-only]" : "");
        if ("log".equals(mode)) return true;

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"이 기능을 사용할 권한이 없습니다.\"}");
        return false;
    }

    private static String path(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) path = path.substring(context.length());
        return path;
    }
}
