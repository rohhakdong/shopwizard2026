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
                        "/payment/**",             // 토스페이먼츠 결제 승인
                        "/profile/cust",           // 회원가입 POST
                        "/profile/cust/login",
                        "/profile/cust/logout",
                        "/profile/cust/byLoginId",
                        // 고객 쇼핑 공개 API
                        "/product/prod/list",
                        "/product/prod/count",
                        "/product/prod",
                        "/product/proditem/list",
                        "/catalog/prod-item/list",
                        "/company/shop",
                        // 고객 인증 필요 API (별도 인터셉터)
                        "/order/basket/**",
                        "/order/order",
                        "/order/orderprod/list",
                        "/order/orderprod/count",
                        "/profile/cust/*/update"
                );

        // 고객 API 인증 체크: cust_id 쿠키 필요
        registry.addInterceptor(new AuthInterceptor("cust_id"))
                .addPathPatterns(
                        "/order/basket/**",
                        "/order/orderprod/list",
                        "/order/orderprod/count"
                );
    }
}
