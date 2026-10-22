package com.shopwizard.order.service;

import com.shopwizard.order.mapper.CheckoutPricingMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.model.PricingItem;
import com.shopwizard.order.model.PricingProd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** 주문 금액은 클라이언트가 아니라 서버가 상품 테이블로 계산한다. */
@ExtendWith(MockitoExtension.class)
class CheckoutPricingServiceTest {

    @Mock private CheckoutPricingMapper mapper;
    private CheckoutPricingService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutPricingService(mapper);
    }

    private static PricingProd prod(String code, String shop, int sale, String feeType, int fee) {
        PricingProd p = new PricingProd();
        p.setProdCode(code);
        p.setProdName("상품" + code);
        p.setShopCode(shop);
        p.setSaleYn(1);
        p.setState(1);
        p.setListPrice(BigDecimal.valueOf(sale + 500));
        p.setSalePrice(BigDecimal.valueOf(sale));
        p.setDeliFeeType(feeType);
        p.setDeliFeeAmt(BigDecimal.valueOf(fee));
        return p;
    }

    private static PricingItem item(String code, int optionPrice) {
        PricingItem i = new PricingItem();
        i.setItemCode(code);
        i.setItemName("20000".equals(code) ? "선택사항없음" : "옵션" + code);
        i.setSaleYn(1);
        i.setSupplyQty(9_999_999);   // 개발 DB 의 옵션 재고(미관리)와 같은 값
        i.setOptionPrice(BigDecimal.valueOf(optionPrice));
        return i;
    }

    private static CheckoutProdItem in(String prodCode, String itemCode, Integer qty) {
        CheckoutProdItem c = new CheckoutProdItem();
        c.setProdCode(prodCode);
        c.setItemCode(itemCode);
        c.setProdQty(qty);
        return c;
    }

    /** 옵션 없는 상품(기본행 20000 만 있음)을 등록한다. */
    private void givenSimple(PricingProd p) {
        lenient().when(mapper.selectProd(p.getProdCode())).thenReturn(p);
        lenient().when(mapper.countRealOptions(p.getProdCode())).thenReturn(0);
        lenient().when(mapper.selectItem(p.getProdCode(), "20000")).thenReturn(item("20000", 0));
    }

    @Test
    void 단가는_상품_판매가이고_합계는_수량을_곱한다() {
        givenSimple(prod("P1", "S1", 10000, "무료", 0));

        PricedOrder o = service.price(List.of(in("P1", "20000", 3)));

        assertThat(o.getLines()).hasSize(1);
        PricedLine l = o.getLines().get(0);
        assertThat(l.getSalePrice()).isEqualTo(10000);
        assertThat(l.getOptionPrice()).isZero();
        assertThat(l.getUnitPrice()).isEqualTo(10000);
        assertThat(l.getLineAmt()).isEqualTo(30000);
        assertThat(l.getListPrice()).isEqualTo(10500);
        assertThat(o.getItemAmt()).isEqualTo(30000);
        assertThat(o.getDeliAmt()).isZero();
        assertThat(o.getTotalAmt()).isEqualTo(30000);
    }

    @Test
    void 클라이언트가_보낸_가격_상품명_상점은_무시하고_서버값을_쓴다() {
        givenSimple(prod("P1", "S1", 10000, "무료", 0));
        CheckoutProdItem forged = in("P1", "20000", 1);
        forged.setSalePrice(1);
        forged.setProdName("위조");
        forged.setShopCode("FORGED");
        forged.setOptionPrice(-9999);

        PricedOrder o = service.price(List.of(forged));

        PricedLine l = o.getLines().get(0);
        assertThat(l.getSalePrice()).isEqualTo(10000);
        assertThat(l.getProdName()).isEqualTo("상품P1");
        assertThat(l.getShopCode()).isEqualTo("S1");
        assertThat(l.getOptionPrice()).isZero();
        assertThat(o.getTotalAmt()).isEqualTo(10000);
    }

    @Test
    void 옵션_추가금은_단가에_더한다() {
        PricingProd p = prod("P2", "S1", 10000, "무료", 0);
        when(mapper.selectProd("P2")).thenReturn(p);
        lenient().when(mapper.countRealOptions("P2")).thenReturn(2);
        when(mapper.selectItem("P2", "20001")).thenReturn(item("20001", 3000));

        PricedOrder o = service.price(List.of(in("P2", "20001", 2)));

        PricedLine l = o.getLines().get(0);
        assertThat(l.getSalePrice()).isEqualTo(10000);
        assertThat(l.getOptionPrice()).isEqualTo(3000);
        assertThat(l.getUnitPrice()).isEqualTo(13000);
        assertThat(l.getLineAmt()).isEqualTo(26000);
        assertThat(l.getItemName()).isEqualTo("옵션20001");
        assertThat(o.getTotalAmt()).isEqualTo(26000);
    }

    @Test
    void 옵션코드를_안_보낸_옵션_없는_상품은_기본행_20000_으로_채운다() {
        givenSimple(prod("P1", "S1", 10000, "무료", 0));

        PricedOrder o = service.price(List.of(in("P1", null, 1)));

        assertThat(o.getLines().get(0).getItemCode()).isEqualTo("20000");
    }

    @Test
    void 진짜_옵션이_있는_상품을_옵션_없이_또는_기본행으로_사는_것은_거절한다() {
        PricingProd p = prod("P2", "S1", 10000, "무료", 0);
        when(mapper.selectProd("P2")).thenReturn(p);
        when(mapper.countRealOptions("P2")).thenReturn(3);

        assertThatThrownBy(() -> service.price(List.of(in("P2", null, 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("옵션을 선택");
        assertThatThrownBy(() -> service.price(List.of(in("P2", "20000", 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("옵션을 선택");
    }

    @Test
    void 선불_배송비는_상점별_최대값을_합산한다() {
        givenSimple(prod("A1", "S1", 10000, "선불", 2500));
        givenSimple(prod("A2", "S1", 20000, "선불", 3000));   // 같은 상점 → 둘 중 큰 3000 한 번
        givenSimple(prod("B1", "S2", 5000, "선불", 2500));    // 다른 상점 → 2500 추가

        PricedOrder o = service.price(List.of(in("A1", "20000", 1), in("A2", "20000", 1), in("B1", "20000", 2)));

        assertThat(o.getItemAmt()).isEqualTo(10000 + 20000 + 10000);
        assertThat(o.getDeliAmt()).isEqualTo(3000 + 2500);
        assertThat(o.getTotalAmt()).isEqualTo(40000 + 5500);
    }

    @Test
    void 무료와_착불은_배송비를_받지_않는다() {
        givenSimple(prod("F1", "S1", 10000, "무료", 0));
        givenSimple(prod("C1", "S2", 10000, "착불", 3000));   // 착불은 금액이 있어도 결제 시점에는 0

        PricedOrder o = service.price(List.of(in("F1", "20000", 1), in("C1", "20000", 1)));

        assertThat(o.getDeliAmt()).isZero();
        assertThat(o.getTotalAmt()).isEqualTo(20000);
    }

    @Test
    void 같은_상점에_무료와_선불이_섞이면_선불_금액만_받는다() {
        givenSimple(prod("F1", "S1", 10000, "무료", 0));
        givenSimple(prod("P1", "S1", 10000, "선불", 2500));

        PricedOrder o = service.price(List.of(in("F1", "20000", 1), in("P1", "20000", 1)));

        assertThat(o.getDeliAmt()).isEqualTo(2500);
    }

    @Test
    void 없는_상품_판매중이_아닌_상품은_거절한다() {
        when(mapper.selectProd("NONE")).thenReturn(null);
        PricingProd stopped = prod("P3", "S1", 10000, "무료", 0);
        stopped.setSaleYn(0);
        when(mapper.selectProd("P3")).thenReturn(stopped);
        PricingProd soldOut = prod("P4", "S1", 10000, "무료", 0);
        soldOut.setSaleYn(-9);
        when(mapper.selectProd("P4")).thenReturn(soldOut);
        PricingProd off = prod("P5", "S1", 10000, "무료", 0);
        off.setState(0);
        when(mapper.selectProd("P5")).thenReturn(off);

        for (String code : new String[] {"NONE", "P3", "P4", "P5"}) {
            assertThatThrownBy(() -> service.price(List.of(in(code, "20000", 1)))).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void 가격이_0인_가격문의_상품은_주문할_수_없다() {
        givenSimple(prod("Z1", "S1", 0, "무료", 0));

        assertThatThrownBy(() -> service.price(List.of(in("Z1", "20000", 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("가격 문의");
    }

    @Test
    void 판매_중지된_옵션_품절_옵션_없는_옵션은_거절한다() {
        PricingProd p = prod("P2", "S1", 10000, "무료", 0);
        when(mapper.selectProd("P2")).thenReturn(p);
        lenient().when(mapper.countRealOptions("P2")).thenReturn(3);
        PricingItem stopped = item("20001", 0);
        stopped.setSaleYn(0);
        PricingItem soldOut = item("20002", 0);
        soldOut.setSupplyQty(0);
        when(mapper.selectItem("P2", "20001")).thenReturn(stopped);
        when(mapper.selectItem("P2", "20002")).thenReturn(soldOut);
        when(mapper.selectItem("P2", "99999")).thenReturn(null);

        for (String code : new String[] {"20001", "20002", "99999"}) {
            assertThatThrownBy(() -> service.price(List.of(in("P2", code, 1)))).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void 수량_범위와_라인_수를_검증한다() {
        givenSimple(prod("P1", "S1", 1000, "무료", 0));

        for (Integer qty : new Integer[] {null, 0, -1, 1000}) {
            assertThatThrownBy(() -> service.price(List.of(in("P1", "20000", qty)))).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> service.price(List.of())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.price(null)).isInstanceOf(ResponseStatusException.class);
        List<CheckoutProdItem> many = new ArrayList<>();
        for (int i = 0; i < 51; i++) many.add(in("P1", "20000", 1));
        assertThatThrownBy(() -> service.price(many)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.price(many.subList(0, 50)).getLines()).hasSize(50);
    }

    @Test
    void 금액이_int_범위를_넘으면_거절한다() {
        givenSimple(prod("BIG", "S1", 489000, "무료", 0));

        assertThatThrownBy(() -> service.price(List.of(in("BIG", "20000", 999), in("BIG", "20000", 999), in("BIG", "20000", 999),
                in("BIG", "20000", 999), in("BIG", "20000", 999), in("BIG", "20000", 999))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("너무 큽니다");
    }

    @Test
    void 소수점_가격은_거절한다() {
        PricingProd p = prod("P1", "S1", 1000, "무료", 0);
        p.setSalePrice(new BigDecimal("1000.50"));
        givenSimple(p);

        assertThatThrownBy(() -> service.price(List.of(in("P1", "20000", 1)))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 재고가_0이하이면_품절_수량이_재고보다_많으면_부족으로_거절한다() {
        PricingProd zero = prod("S0", "S1", 1000, "무료", 0);
        zero.setSupplyQty(0);
        givenSimple(zero);
        PricingProd few = prod("S3", "S1", 1000, "무료", 0);
        few.setSupplyQty(3);
        givenSimple(few);

        assertThatThrownBy(() -> service.price(List.of(in("S0", "20000", 1))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("품절");
        assertThatThrownBy(() -> service.price(List.of(in("S3", "20000", 4))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("재고가 부족").hasMessageContaining("3개");
        assertThat(service.price(List.of(in("S3", "20000", 3))).getTotalAmt()).isEqualTo(3000);   // 정확히 재고만큼은 허용
    }

    @Test
    void 같은_상품을_여러_라인에_나눠도_합계로_재고를_본다() {
        PricingProd few = prod("S3", "S1", 1000, "무료", 0);
        few.setSupplyQty(3);
        givenSimple(few);

        assertThatThrownBy(() -> service.price(List.of(in("S3", "20000", 2), in("S3", "20000", 2))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("재고가 부족");
    }

    @Test
    void 재고가_null_이면_재고_미관리로_보고_통과한다() {
        PricingProd unmanaged = prod("SN", "S1", 1000, "무료", 0);
        unmanaged.setSupplyQty(null);
        givenSimple(unmanaged);

        assertThat(service.price(List.of(in("SN", "20000", 999))).getTotalAmt()).isEqualTo(999000);
    }

    @Test
    void 옵션_재고보다_많이_주문하면_거절한다() {
        PricingProd p = prod("P2", "S1", 10000, "무료", 0);
        when(mapper.selectProd("P2")).thenReturn(p);
        lenient().when(mapper.countRealOptions("P2")).thenReturn(2);
        PricingItem opt = item("20001", 0);
        opt.setSupplyQty(2);
        when(mapper.selectItem("P2", "20001")).thenReturn(opt);

        assertThatThrownBy(() -> service.price(List.of(in("P2", "20001", 3))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("옵션 재고");
        assertThat(service.price(List.of(in("P2", "20001", 2))).getTotalAmt()).isEqualTo(20000);
    }
}
