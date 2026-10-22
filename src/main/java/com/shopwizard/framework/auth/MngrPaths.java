package com.shopwizard.framework.auth;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 관리자(MNGR) 세션이 보호하는 경로 목록. 인증(AuthInterceptor)·역할 권한(RoleAccessInterceptor)·
 * 데이터 범위(DataScopeFilter) 세 곳이 같은 목록을 써야 서로 어긋나지 않으므로 한 곳에 둔다.
 */
public final class MngrPaths {

    private MngrPaths() {}

    /** 로그인 없이 GET/HEAD 로만 여는 스토어프론트 공개 조회. */
    public static final String[] PUBLIC_READ = {
            "/product/prod",
            "/product/prod/list",
            "/product/prod/count",
            "/product/proditem/list",
            "/catalog/prod-item/list",
            "/company/shop/public"
    };

    public static final String[] PATHS = {
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
    };

    public static final String[] EXCLUDES = {
            "/auth/**",
            "/payment/**",             // 토스페이먼츠 결제 승인/웹훅
            "/profile/cust/register",  // 비회원의 자기 회원가입 POST (관리자용 POST /profile/cust 와는 분리됨)
            // 고객 인증 필요 API (CUST 인터셉터로 보호)
            "/order/basket/**",
            "/order/checkout",         // 토스페이먼츠 결제완료 콜백(주문 저장) — 비회원도 허용
            "/order/checkout/quote",   // 결제 전 서버 금액 견적 — 비회원도 허용(부작용 없음)
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
    };

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 이 경로가 관리자 보호 대상(인터셉터가 걸리는 경로)인가 — PATHS 에 맞고 EXCLUDES 에는 맞지 않음. */
    public static boolean isMngrProtected(String path) {
        for (String p : EXCLUDES) if (MATCHER.match(p, path)) return false;
        for (String p : PATHS) if (MATCHER.match(p, path)) return true;
        return false;
    }

    public static boolean isPublicRead(String path) {
        return List.of(PUBLIC_READ).stream().anyMatch(p -> MATCHER.match(p, path));
    }
}
