package com.shopwizard.config;

import com.shopwizard.framework.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 관리자 API 인증 체크: mngr_loginId 쿠키 필요
        registry.addInterceptor(new AuthInterceptor("mngr_loginId"))
                .addPathPatterns(
                        "/authority/**",
                        "/catalog/**",
                        "/code/**",
                        "/company/**",
                        "/order/**",
                        "/ship/**",
                        "/stock/**",
                        "/adjust/**",
                        "/log/**",
                        "/message/**",
                        "/operate/**",
                        "/product/**",
                        "/statistics/**",
                        "/store/**",
                        "/profile/**"
                )
                .excludePathPatterns(
                        "/auth/**",
                        "/profile/cust/login",
                        "/profile/cust/logout"
                );
    }
}
