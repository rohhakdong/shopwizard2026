package com.shopwizard.framework.auth;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAccessPolicyTest {

    private static RolePermissions perms(Set<String> view, Set<String> write) {
        return new RolePermissions(view, write);
    }

    /** 송장입력만 쓰기, 주문관리는 조회만 가진 역할. */
    private final RolePermissions limited = perms(
            Set.of("order-list", "ship-direct", "ship-direct-check"),
            Set.of("ship-direct"));

    private static boolean ok(String method, String path, RolePermissions p) {
        return RoleAccessPolicy.decide(method, path, p).allowed();
    }

    @Test
    void 소유_화면의_조회_권한으로_GET_허용() {
        assertTrue(ok("GET", "/order/orderprod/list", limited));
        assertTrue(ok("GET", "/ship/ship-direct/list", limited));
    }

    @Test
    void 조회_권한이_없는_화면의_GET_은_403() {
        assertFalse(ok("GET", "/profile/cust/list", limited));
        assertFalse(ok("GET", "/adjust/adjust/list", limited));
        assertFalse(ok("GET", "/authority/role/list", limited));
    }

    @Test
    void 쓰기는_write_권한이_있어야_한다() {
        assertTrue(ok("PUT", "/ship/ship-direct/invoice", limited));
        // 발주확인은 조회만 가능
        assertFalse(ok("POST", "/ship/ship-direct/check-batch", limited));
        // 주문관리는 조회만 가능 → cancel-batch(주문관리·주문취소접수 공유) 거부
        assertFalse(ok("POST", "/order/order/cancel-batch", limited));
    }

    @Test
    void 공유_엔드포인트는_소유_화면_중_하나만_있으면_통과() {
        RolePermissions cancelOnly = perms(Set.of("order-cancel"), Set.of("order-cancel"));
        assertTrue(ok("POST", "/order/order/cancel-batch", cancelOnly));
    }

    @Test
    void 경로_변수_엔드포인트도_소유표로_매칭된다() {
        RolePermissions brand = perms(Set.of("catalog-brand"), Set.of("catalog-brand"));
        assertTrue(ok("DELETE", "/catalog/brand/B001", brand));
        assertFalse(ok("DELETE", "/catalog/maker/M001", brand));
    }

    @Test
    void 열린_조회는_권한_없이도_허용() {
        assertTrue(ok("GET", "/company/shop", RolePermissions.NONE));
        assertTrue(ok("GET", "/company/chnl", RolePermissions.NONE));
        assertTrue(ok("GET", "/code/constr-val", RolePermissions.NONE));
        // 쓰기는 열려 있지 않다
        assertFalse(ok("POST", "/company/shop", RolePermissions.NONE));
    }

    @Test
    void 화면이_쓰지_않는_쓰기_엔드포인트는_거부() {
        assertFalse(ok("POST", "/operate/notice", limited));
        assertFalse(ok("DELETE", "/store/event", limited));
    }

    @Test
    void 화면이_쓰지_않는_GET_은_민감_도메인이면_도메인_화면_조회_권한이_필요() {
        // 주문 도메인 화면을 볼 수 있으면 대시보드용 GET 통과
        assertTrue(ok("GET", "/order/orderprod/state-count", limited));
        assertTrue(ok("GET", "/order/order/orderlist/count", limited));
        // 회원/권한 도메인은 화면 권한이 없으니 거부
        assertFalse(ok("GET", "/authority/mngr/count", limited));
        assertFalse(ok("GET", "/profile/cust/point", limited));
        // 소유 화면이 아예 없는 민감 도메인(정산 외 payment 등)은 일반 역할 거부
        assertFalse(ok("GET", "/payment/log", limited));
    }

    @Test
    void 비민감_도메인의_미사용_GET_은_통과() {
        assertTrue(ok("GET", "/catalog/prod/unused/lookup", RolePermissions.NONE));
        assertTrue(ok("GET", "/company/comp/unused/lookup", RolePermissions.NONE));
    }

    @Test
    void 아무_권한도_없으면_화면_API_는_모두_거부() {
        assertFalse(ok("GET", "/order/orderprod/list", RolePermissions.NONE));
        assertFalse(ok("PUT", "/ship/ship-direct/invoice", RolePermissions.NONE));
    }

    @Test
    void 상품_승인은_승인_화면_권한이_있어야_한다() {
        RolePermissions regOnly = perms(Set.of("catalog-prod"), Set.of("catalog-prod"));
        assertFalse(ok("PUT", "/catalog/prod/approv", regOnly));
        assertFalse(ok("POST", "/catalog/prod/copy2shopion", regOnly));
        assertTrue(ok("POST", "/catalog/prod", regOnly));

        RolePermissions approver = perms(Set.of("catalog-prod", ScreenRegistry.APPROVE),
                Set.of("catalog-prod", ScreenRegistry.APPROVE));
        assertTrue(ok("PUT", "/catalog/prod/approv", approver));
    }
}
