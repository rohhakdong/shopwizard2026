package com.shopwizard.framework.interceptor;

import com.shopwizard.framework.auth.AuthSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 해당 주체({@link AuthSession.Type})로 로그인한 서버 세션이 있어야 통과시킨다. 예전에는 같은 이름의
 * 쿠키가 "있기만 하면" 통과시켜서 Cookie 헤더 위조만으로 인증이 뚫렸다.
 *
 * <p>{@code publicReadPatterns}에 해당하는 경로는 GET/HEAD 요청에 한해 로그인 없이 통과시킨다.
 * 스프링의 excludePathPatterns 는 HTTP 메서드를 구분하지 못해 같은 경로의 POST/PUT/DELETE까지
 * 통째로 인증 없이 열어버리므로(실제로 /product/prod 의 등록·수정·삭제가 열려 있었음), "공개 조회"는
 * 제외 목록이 아니라 여기서 메서드까지 확인해서 허용한다.</p>
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthSession.Type type;
    private final List<String> publicReadPatterns;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public AuthInterceptor(AuthSession.Type type, String... publicReadPatterns) {
        this.type = type;
        this.publicReadPatterns = List.of(publicReadPatterns);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (isPublicRead(request)) return true;
        if (AuthSession.isLoggedIn(request, type)) return true;

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
        return false;
    }

    private boolean isPublicRead(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) return false;
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        for (String pattern : publicReadPatterns) {
            if (matcher.match(pattern, path)) return true;
        }
        return false;
    }
}
