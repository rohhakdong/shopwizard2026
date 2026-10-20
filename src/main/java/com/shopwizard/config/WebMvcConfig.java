package com.shopwizard.config;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // application.yml의 upload.dir (상대경로면 실행 위치 기준). 상품이미지 업로드 파일이 저장되는 루트.
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /**
     * 업로드된 상품이미지(원본+대/중/소 등 6단계 썸네일)를 정적 리소스로 서빙한다.
     * 실제 데이터의 기존 URL 패턴(/images/product/{shopCode}/{yyyymmdd}/{prodCode}...)과
     * 맞추기 위해 /images/** 로 매핑한다. classpath:/static/images/ 는 현재 비어 있어 충돌하지 않는다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/images/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 관리자 API 인증 체크: 관리자(MNGR)로 로그인한 서버 세션 필요.
        // 예전에는 mngr_loginId 쿠키가 "있기만 하면" 통과시켜 Cookie 헤더 위조로 뚫렸다(AuthSession 참고).
        //
        // 스토어프론트용 "공개 조회"는 excludePathPatterns 가 아니라 publicReadPatterns(GET/HEAD 한정)로
        // 연다 — 제외 목록은 HTTP 메서드를 구분하지 못해 같은 경로의 POST/PUT/DELETE까지 인증 없이 열린다
        // (실제로 /product/prod 의 상품 등록·수정·삭제와 /company/shop 전체가 그렇게 열려 있었다).
        // /company/shop/public 은 shopCode/shopName만 반환하는 별도 경로라 전체 필드(loginId/passwd 포함)가
        // 새지 않는다.
        registry.addInterceptor(new AuthInterceptor(AuthSession.Type.MNGR,
                        "/product/prod",
                        "/product/prod/list",
                        "/product/prod/count",
                        "/product/proditem/list",
                        "/catalog/prod-item/list",
                        "/company/shop/public"))
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
                        "/payment/**",             // 토스페이먼츠 결제 승인/웹훅
                        "/profile/cust/register",  // 비회원의 자기 회원가입 POST (관리자용 POST /profile/cust 와는 분리됨)
                        // 고객 인증 필요 API (아래 CUST 인터셉터로 보호)
                        "/order/basket/**",
                        "/order/checkout",         // 토스페이먼츠 결제완료 콜백(주문 저장) — 비회원도 허용
                        "/order/orderprod/my/list",
                        "/order/orderprod/my/list/count",
                        // 비회원 주문조회 (주문번호+연락처로 본인 확인, 로그인 불필요)
                        "/order/orderprod/guest/list",
                        // 로그인한 회원 본인의 내 정보 수정 (CUST 인터셉터로 보호)
                        "/profile/cust/me",
                        // 상점(거래처) 계정 전용 API (SHOP 인터셉터로 보호)
                        "/company/shop/me",
                        "/order/orderprod/shop/list",
                        "/order/orderprod/shop/list/count",
                        // 창고(거래처) 계정 전용 API (WAREHS 인터셉터로 보호)
                        "/company/warehs/me"
                );

        // 고객 API 인증 체크: 고객(CUST)으로 로그인한 서버 세션 필요
        // 주의: /order/orderprod/list, /list/count 는 관리자(order-list.js)도 쓰는 공용
        // 엔드포인트라 여기 넣으면 안 됨 — 관리자 세션엔 고객 로그인이 없어 401이 난다.
        // 고객용은 /order/orderprod/my/list, /my/list/count 로 경로를 분리해뒀다.
        // /order/checkout 은 여기 넣지 않는다 — 비회원 주문(로그인 없이 결제)을 허용해야 하므로
        // 고객 로그인 유무는 CheckoutController가 직접 보고 회원/비회원을 분기한다.
        registry.addInterceptor(new AuthInterceptor(AuthSession.Type.CUST))
                .addPathPatterns(
                        "/order/basket/**",
                        "/order/orderprod/my/list",
                        "/order/orderprod/my/list/count",
                        "/profile/cust/me"
                );

        // 상점(거래처) 계정 API 인증 체크: 상점(SHOP)으로 로그인한 서버 세션 필요
        // 관리자/회원과 완전히 분리된 별도 로그인이라, 여기 걸린 경로는 관리자/고객 로그인만으로는
        // 접근할 수 없다.
        registry.addInterceptor(new AuthInterceptor(AuthSession.Type.SHOP))
                .addPathPatterns(
                        "/company/shop/me",
                        "/order/orderprod/shop/list",
                        "/order/orderprod/shop/list/count"
                );

        // 창고(거래처) 계정 API 인증 체크: 창고(WAREHS)로 로그인한 서버 세션 필요
        // 관리자/회원/상점과 완전히 분리된 별도 로그인이라, 여기 걸린 경로는 다른 로그인만으로는
        // 접근할 수 없다. 현재 범위는 본인 정보 조회까지 (로그인 + 내 정보).
        registry.addInterceptor(new AuthInterceptor(AuthSession.Type.WAREHS))
                .addPathPatterns(
                        "/company/warehs/me"
                );
    }
}
