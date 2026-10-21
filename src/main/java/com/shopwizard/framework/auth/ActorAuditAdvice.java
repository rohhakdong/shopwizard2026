package com.shopwizard.framework.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * {@code @RequestBody} 를 읽은 직후, 세션으로 인증된 요청이면 바디의 감사 필드(registId/registName,
 * changeId/changeName)를 로그인한 행위자의 값으로 덮어쓴다({@link AuditFields} 참고). 엔드포인트마다 손대지
 * 않아도 모든 쓰기 API 에 적용되도록 중앙에서 처리한다.
 *
 * <p>행위자는 인터셉터가 세션 인증에 성공했을 때만 요청에 표시하므로({@link AuthSession#markActor}), 인증을 거치지
 * 않는 공개 경로(회원가입, 결제 콜백, 웹훅)의 바디는 건드리지 않는다.</p>
 */
@ControllerAdvice
public class ActorAuditAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            AuditFields.apply(body, AuthSession.actor(request), request.getMethod());
        }
        return body;
    }
}
