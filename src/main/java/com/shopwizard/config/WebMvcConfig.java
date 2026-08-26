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
                        "/order/checkout",         // 토스페이먼츠 결제완료 콜백(주문 저장) — 비회원도 허용
                        "/order/orderprod/my/list",
                        "/order/orderprod/my/list/count",
                        // 비회원 주문조회 (주문번호+연락처로 본인 확인, 로그인 불필요)
                        "/order/orderprod/guest/list",
                        "/profile/cust/*/update"
                );

        // 고객 API 인증 체크: cust_id 쿠키 필요
        // 주의: /order/orderprod/list, /list/count 는 관리자(order-list.js)도 쓰는 공용
        // 엔드포인트라 여기 넣으면 안 됨 — 관리자 세션엔 cust_id 쿠키가 없어 401이 난다.
        // 고객용은 /order/orderprod/my/list, /my/list/count 로 경로를 분리해뒀다.
        // /order/checkout 은 여기 넣지 않는다 — 비회원 주문(로그인 없이 결제)을 허용해야 하므로
        // cust_id 쿠키 유무는 CheckoutController가 직접 보고 회원/비회원을 분기한다.
        registry.addInterceptor(new AuthInterceptor("cust_id"))
                .addPathPatterns(
                        "/order/basket/**",
                        "/order/orderprod/my/list",
                        "/order/orderprod/my/list/count"
                );
    }
}
