package com.shopwizard.framework.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 관리자 세션이 보호하는 경로 목록 — 고객 전용 경로는 관리자 인증·역할 검사의 대상이 아니어야 한다(관리자 세션이 없어 401/403 이 난다). */
class MngrPathsTest {

    @Test
    void 고객_본인_배송지_경로는_관리자_보호_대상이_아니다() {
        for (String p : new String[]{
                "/profile/cust/me",
                "/profile/cust/me/deli-addrs",
                "/profile/cust/me/deli-addrs/7",
                "/profile/cust/me/deli-addrs/7/default"}) {
            assertFalse(MngrPaths.isMngrProtected(p), p);
        }
    }

    @Test
    void 스토어프론트_사업자_정보는_공개_조회_경로다() {
        assertTrue(MngrPaths.isPublicRead("/company/shop/business-info"));
        assertFalse(MngrPaths.isPublicRead("/company/shop"));      // 관리자용 상점 목록은 공개가 아니다
        assertFalse(MngrPaths.isPublicRead("/company/comp"));      // 관리자용 회사 목록도 마찬가지
    }

    @Test
    void 관리자용_회원_배송지_경로는_계속_관리자_보호_대상이다() {
        for (String p : new String[]{
                "/profile/custDeliAddr/list",
                "/profile/custDeliAddr/default/2",
                "/profile/cust/list",
                "/profile/cust/2"}) {
            assertTrue(MngrPaths.isMngrProtected(p), p);
        }
    }
}
