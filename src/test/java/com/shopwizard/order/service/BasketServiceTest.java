package com.shopwizard.order.service;

import com.shopwizard.order.mapper.BasketMapper;
import com.shopwizard.order.model.Basket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock private BasketMapper basketMapper;
    @InjectMocks private BasketService service;

    private static Basket req(String prodCode, String itemCode, Integer qty) {
        Basket b = new Basket();
        b.setProdCode(prodCode);
        b.setItemCode(itemCode);
        b.setProdQty(qty);
        // 클라이언트가 보낸 가격/이름은 무시되어야 한다
        b.setSalePrice(1);
        b.setProdName("조작된 이름");
        return b;
    }

    @Test
    void 새_상품은_세션_고객의_행으로_담긴다() {
        when(basketMapper.countProd("1000000342")).thenReturn(1);
        when(basketMapper.selectByItem(any())).thenReturn(null);
        // 실제로는 useGeneratedKeys 가 채워주는 basketNo 를 흉내낸다
        doAnswer(inv -> { ((Basket) inv.getArgument(0)).setBasketNo(42); return null; })
                .when(basketMapper).insert(any());
        when(basketMapper.select(any())).thenReturn(new Basket());

        service.add("7", "login7", "홍길동", req("1000000342", "20000", 2));

        ArgumentCaptor<Basket> row = ArgumentCaptor.forClass(Basket.class);
        verify(basketMapper).insert(row.capture());
        assertThat(row.getValue().getCustId()).isEqualTo("7");
        assertThat(row.getValue().getProdCode()).isEqualTo("1000000342");
        assertThat(row.getValue().getItemCode()).isEqualTo("20000");
        assertThat(row.getValue().getProdQty()).isEqualTo(2);
        assertThat(row.getValue().getRegistId()).isEqualTo("login7");
        // 클라이언트가 보낸 가격/이름은 새 행에 옮겨 담지 않는다(조회 시 상품 테이블에서 채움)
        assertThat(row.getValue().getSalePrice()).isNull();
        assertThat(row.getValue().getProdName()).isNull();
    }

    @Test
    void 같은_상품_옵션이_이미_있으면_새_행_대신_수량을_합산한다() {
        when(basketMapper.countProd("1000000342")).thenReturn(1);
        Basket existing = new Basket();
        existing.setBasketNo(11);
        existing.setProdQty(3);
        when(basketMapper.selectByItem(any())).thenReturn(existing);
        when(basketMapper.update(any())).thenReturn(1);
        when(basketMapper.select(any())).thenReturn(existing);

        service.add("7", "login7", "홍길동", req("1000000342", "20000", 2));

        verify(basketMapper, never()).insert(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> upd = ArgumentCaptor.forClass(Map.class);
        verify(basketMapper).update(upd.capture());
        assertThat(upd.getValue()).containsEntry("prodQty", 5).containsEntry("basketNo", 11).containsEntry("custId", "7");
    }

    @Test
    void 합산해도_상한을_넘지_않는다() {
        when(basketMapper.countProd("1000000342")).thenReturn(1);
        Basket existing = new Basket();
        existing.setBasketNo(11);
        existing.setProdQty(990);
        when(basketMapper.selectByItem(any())).thenReturn(existing);
        when(basketMapper.update(any())).thenReturn(1);
        when(basketMapper.select(any())).thenReturn(existing);

        service.add("7", "l", "n", req("1000000342", null, 50));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> upd = ArgumentCaptor.forClass(Map.class);
        verify(basketMapper).update(upd.capture());
        assertThat(upd.getValue()).containsEntry("prodQty", BasketService.MAX_QTY);
    }

    @Test
    void 없는_상품은_담을_수_없다() {
        when(basketMapper.countProd("nope")).thenReturn(0);

        assertThatThrownBy(() -> service.add("7", "l", "n", req("nope", null, 1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(basketMapper, never()).insert(any());
    }

    @Test
    void 수량_범위를_벗어나면_거절한다() {
        when(basketMapper.countProd("1000000342")).thenReturn(1);

        assertThatThrownBy(() -> service.add("7", "l", "n", req("1000000342", null, 0)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.add("7", "l", "n", req("1000000342", null, 1000)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.changeQty("7", 1, -1, "l", "n"))
                .isInstanceOf(ResponseStatusException.class);
        verify(basketMapper, never()).insert(any());
    }

    @Test
    void 남의_장바구니_수량변경은_0행이라_404다() {
        when(basketMapper.update(any())).thenReturn(0);

        assertThatThrownBy(() -> service.changeQty("7", 99, 2, "l", "n"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void 삭제와_비우기는_항상_고객ID_조건과_함께_나간다() {
        service.delete("7", 5);
        service.clean("7");
        service.deleteMulti("7", List.of(1, 2));

        verify(basketMapper).delete(Map.of("custId", "7", "basketNo", 5));
        verify(basketMapper).clean(Map.of("custId", "7"));
        verify(basketMapper).deleteMulti(Map.of("custId", "7", "pBasketNoArr", List.of(1, 2)));
    }
}
