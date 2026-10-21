package com.shopwizard.framework.auth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionsTest {

    private static Map<String, Object> menu(String code, String uri) {
        return Map.of("MenuCode", code, "MenuUri", uri);
    }

    private static Map<String, Object> functn(String code, String uri) {
        return Map.of("MenuCode", code, "FunctnUri", uri);
    }

    /** 업체담당자 역할의 실제 데이터 모양(일부). */
    private RolePermissions supplier() {
        return RolePermissions.compute(
                List.of(
                        menu("1001000000000", ""),
                        menu("1001001000000", "/wizardn/catalog/wCatProdList.wzn"),
                        menu("1002001000000", "/wizardn/order/wOrdOrderProdList.wzn"),
                        menu("1003001000000", "/wizardn/ship/wShpShipDirectList.wzn"),
                        menu("1003002000000", "/wizardn/ship/wShpShipInvoiceList.wzn"),
                        menu("1003003000000", "/wizardn/ship/wShpReturnDirectList.wzn")),
                List.of(
                        functn("1001001000000", "/wizardn/catalog/wCatProdList.wzn"),
                        functn("1001001000000", "/wizardn/catalog/xCatProdInsert.wzn"),
                        functn("1001001000000", "/wizardn/product/wPrdProdList.wzn"),
                        functn("1002001000000", "/wizardn/order/wOrdOrderProdList.wzn"),
                        functn("1002001000000", "/wizardn/order/rOrdOrderProdList.xls"),
                        functn("1003001000000", "/wizardn/ship/xShpShipDirectUpdate.wzn"),
                        functn("1003002000000", "/wizardn/ship/xShpShipInvoiceUpdate.wzn"),
                        functn("1003003000000", "/wizardn/ship/wShpReturnDirectList.wzn")));
    }

    @Test
    void 메뉴와_기능_URI_로_조회_가능한_화면이_정해진다() {
        RolePermissions p = supplier();
        assertTrue(p.view().containsAll(List.of("catalog-prod", "product-prod", "order-list",
                "ship-direct-check", "ship-direct", "ship-return", "ship-return-request")));
        assertFalse(p.view().contains("profile-cust"));
        assertFalse(p.view().contains("adjust"));
        assertFalse(p.view().contains("auth-role"));
        assertFalse(p.view().contains(ScreenRegistry.APPROVE));
    }

    @Test
    void 쓰기는_x_기능이_있는_메뉴의_화면만() {
        RolePermissions p = supplier();
        assertTrue(p.write().containsAll(List.of("catalog-prod", "product-prod", "ship-direct-check", "ship-direct")));
        // 주문관리·반품확정은 조회 기능만 있고 x 기능이 없다.
        assertFalse(p.write().contains("order-list"));
        assertFalse(p.write().contains("ship-return"));
    }

    @Test
    void 출력용_r_기능은_쓰기가_아니다() {
        RolePermissions p = RolePermissions.compute(
                List.of(menu("m1", "/wizardn/order/wOrdOrderProdList.wzn")),
                List.of(functn("m1", "/wizardn/order/rOrdOrderProdList.xls")));
        assertTrue(p.view().contains("order-list"));
        assertFalse(p.write().contains("order-list"));
    }

    @Test
    void 켜진_메뉴_아래의_기능만_인정한다() {
        // 메뉴가 목록에 없다 = State=0 이라 조회에서 제외됨 → 그 아래 기능은 무시
        RolePermissions p = RolePermissions.compute(
                List.of(),
                List.of(functn("gone", "/wizardn/order/wOrdOrderProdList.wzn"), functn("gone", "/wizardn/order/xOrdOrderProdUpdate.wzn")));
        assertTrue(p.view().isEmpty());
    }

    @Test
    void 다른_메뉴의_쓰기_기능은_빌려오지_않는다() {
        RolePermissions p = RolePermissions.compute(
                List.of(menu("a", "/wizardn/order/wOrdOrderProdList.wzn"), menu("b", "/wizardn/ship/wShpShipDirectList.wzn")),
                List.of(functn("b", "/wizardn/ship/xShpShipDirectUpdate.wzn")));
        assertFalse(p.write().contains("order-list"));
        assertTrue(p.write().contains("ship-direct-check"));
    }

    @Test
    void URI_앞뒤_공백_쿼리스트링_확장자를_정리한다() {
        assertEquals("wCatProdPriceList", RolePermissions.basename(" /wizardn/catalog/wCatProdPriceList.wzn"));
        assertEquals("wPrdProdReviewList", RolePermissions.basename("/wizardn/product/wPrdProdReviewList.wzn\t"));
        assertEquals("wStrLayoutProdList", RolePermissions.basename("/wizardn/store/wStrLayoutProdList.wzn?pLayoutApplyIncluded=YES"));
        assertEquals("", RolePermissions.basename("?"));
        assertEquals("", RolePermissions.basename(null));
    }

    @Test
    void 상품승인_메뉴가_있으면_승인_권한() {
        RolePermissions p = RolePermissions.compute(
                List.of(menu("m", "/wizardn/catalog/wCatProdApprovList.wzn")), List.of());
        assertTrue(p.view().contains(ScreenRegistry.APPROVE));
        assertTrue(p.write().contains(ScreenRegistry.APPROVE));
    }

    @Test
    void 사이드바_모든_화면을_볼_수_있으면_full() {
        List<Map<String, Object>> menus = new ArrayList<>();
        int i = 0;
        for (ScreenRegistry.Screen s : ScreenRegistry.SCREENS) {
            menus.add(menu("m" + i++, "/wizardn/x/" + s.bases().get(0) + ".wzn"));
        }
        assertTrue(RolePermissions.compute(menus, List.of()).full());
        assertFalse(supplier().full());
        assertFalse(RolePermissions.NONE.full());
    }

    @Test
    void 권한_관리_화면에_모두_쓰기_권한이_있으면_모든_화면_무제한() {
        RolePermissions p = RolePermissions.compute(
                List.of(menu("a", "/wizardn/authority/wAthRoleList.wzn"),
                        menu("b", "/wizardn/authority/wAthMenuList.wzn"),
                        menu("c", "/wizardn/authority/wAthFunctnList.wzn")),
                List.of(functn("a", "/wizardn/authority/xAthRoleUpdate.wzn"),
                        functn("b", "/wizardn/authority/xAthMenuUpdate.wzn"),
                        functn("c", "/wizardn/authority/xAthFunctnUpdate.wzn")));
        assertTrue(p.full());
        assertTrue(p.write().contains("adjust"));
        assertTrue(p.write().contains(ScreenRegistry.APPROVE));
    }

    @Test
    void 권한_관리_화면_일부만_있으면_무제한이_아니다() {
        RolePermissions p = RolePermissions.compute(
                List.of(menu("a", "/wizardn/authority/wAthRoleList.wzn")),
                List.of(functn("a", "/wizardn/authority/xAthRoleUpdate.wzn")));
        assertFalse(p.full());
        assertFalse(p.view().contains("adjust"));
    }
}
