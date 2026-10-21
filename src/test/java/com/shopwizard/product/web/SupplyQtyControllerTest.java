package com.shopwizard.product.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.product.service.ProdService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 판매 상품 관리 "재고 수정": 화면은 절대값(supplyQty)과 자기가 보던 값(expectedQty)을 보낸다.
 * 예전에는 차감용 쿼리(SupplyQty - qty)를 호출해 qty 가 없는 이 요청이 재고를 NULL(무제한)로 만들었고,
 * 오래된 화면 값으로 덮어쓰면 그 사이 주문 차감분이 사라졌다.
 */
class SupplyQtyControllerTest {

    private final ProdService prodService = mock(ProdService.class);
    private final DataScopeGuard guard = mock(DataScopeGuard.class);
    private final ProdController controller = new ProdController(prodService, guard);
    private final MockHttpServletRequest http = new MockHttpServletRequest();

    private static Map<String, Object> body(Object supplyQty, boolean withExpected, Object expected) {
        Map<String, Object> m = new HashMap<>();
        m.put("prodCode", "P1");
        m.put("supplyQty", supplyQty);
        if (withExpected) m.put("expectedQty", expected);
        return m;
    }

    @Test
    void 화면이_보던_값과_같으면_절대값으로_설정한다() {
        when(prodService.setSupplyQty(anyMap())).thenReturn(true);
        Map<String, Object> b = body(30, true, 50);

        controller.updateSupplyQty(b, http);

        verify(prodService).setSupplyQty(b);
        verify(prodService, never()).updateSupplyQty(anyMap());   // 차감 쿼리는 쓰지 않는다
    }

    @Test
    void 그_사이_재고가_바뀌었으면_409() {
        when(prodService.setSupplyQty(anyMap())).thenReturn(false);

        assertThatThrownBy(() -> controller.updateSupplyQty(body(30, true, 50), http))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void 재고_미관리_NULL_에서의_설정도_null_expected_로_통과한다() {
        when(prodService.setSupplyQty(anyMap())).thenReturn(true);

        controller.updateSupplyQty(body(10, true, null), http);

        verify(prodService).setSupplyQty(anyMap());
    }

    @Test
    void 수량이_숫자가_아니거나_음수거나_expectedQty_가_없으면_400이고_쓰지_않는다() {
        for (Map<String, Object> bad : new Map[]{body(null, true, 1), body("abc", true, 1), body(-1, true, 1), body(5, false, null)}) {
            assertThatThrownBy(() -> controller.updateSupplyQty(bad, http))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
        verify(prodService, never()).setSupplyQty(anyMap());
    }
}
