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
                        "/profile/cust/register",  // 비회원의 자기 회원가입 POST (관리자용 POST /profile/cust 와는 분리됨)
                        "/profile/cust/login",
                        "/profile/cust/logout",
                        "/profile/cust/byLoginId",
                        // 고객 쇼핑 공개 API
                        "/product/prod/list",
                        "/product/prod/count",
                        "/product/prod",
                        "/product/proditem/list",
                        "/catalog/prod-item/list",
                        // shopCode/shopName만 반환하는 공개 목록 — "/company/shop" 자체(전체 필드,
                        // loginId/passwd 포함)를 여기 넣으면 excludePathPatterns가 HTTP 메서드를
                        // 구분하지 못해 POST/PUT/DELETE까지 통째로 인증 없이 뚫린다(실제로 뚫려있던
                        // 버그를 고침) — 경로 자체를 분리해서 공개 범위를 최소화했다.
                        "/company/shop/public",
                        // 고객 인증 필요 API (별도 인터셉터)
                        "/order/basket/**",
                        "/order/order",
                        "/order/checkout",         // 토스페이먼츠 결제완료 콜백(주문 저장) — 비회원도 허용
                        "/order/orderprod/my/list",
                        "/order/orderprod/my/list/count",
                        // 비회원 주문조회 (주문번호+연락처로 본인 확인, 로그인 불필요)
                        "/order/orderprod/guest/list",
                        "/profile/cust/*/update",
                        // 로그인한 회원 본인의 내 정보 수정 (별도 cust_id 인터셉터로 보호)
                        "/profile/cust/me",
                        // 상점(거래처) 계정 전용 API (별도 shop_code 인터셉터로 보호)
                        "/company/shop/me",
                        "/order/orderprod/shop/list",
                        "/order/orderprod/shop/list/count",
                        // 창고(거래처) 계정 전용 API (별도 warehs_code 인터셉터로 보호)
                        "/company/warehs/me"
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
                        "/order/orderprod/my/list/count",
                        "/profile/cust/me"
                );

        // 상점(거래처) 계정 API 인증 체크: shop_code 쿠키 필요
        // 관리자/회원과 완전히 분리된 별도 로그인이라, 여기 걸린 경로는 mngr_loginId/cust_id
        // 쿠키만으로는 접근할 수 없다 (관리자는 shop_code 쿠키가 없어 401).
        registry.addInterceptor(new AuthInterceptor("shop_code"))
                .addPathPatterns(
                        "/company/shop/me",
                        "/order/orderprod/shop/list",
                        "/order/orderprod/shop/list/count"
                );

        // 창고(거래처) 계정 API 인증 체크: warehs_code 쿠키 필요
        // 관리자/회원/상점과 완전히 분리된 별도 로그인이라, 여기 걸린 경로는 다른 쿠키만으로는
        // 접근할 수 없다. 현재 범위는 본인 정보 조회까지 (로그인 + 내 정보).
        registry.addInterceptor(new AuthInterceptor("warehs_code"))
                .addPathPatterns(
                        "/company/warehs/me"
                );
    }
}
