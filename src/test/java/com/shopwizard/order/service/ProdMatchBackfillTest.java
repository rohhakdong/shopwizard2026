package com.shopwizard.order.service;

import com.shopwizard.order.model.OrderProd;
import com.shopwizard.product.model.Prod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 매칭/샵링커 자동매칭의 라인 금액 보정(backfillLine). 주문 라인의 금액은 모두 "수량이 곱해진 라인 합계"이고
 * 상품마스터의 공급가·원가는 단가라서, 마스터 값을 넣을 때는 수량을 곱해야 한다.
 */
class ProdMatchBackfillTest {

    private static Prod prod(int supply, int buy, int vat) {
        Prod p = new Prod();
        p.setProdCode("1000000017");
        p.setSupplyPrice(supply);
        p.setBuyPrice(buy);
        p.setVatRate(vat);
        return p;
    }

    private static OrderProd line(int qty, Integer sale, Integer nv, Integer supply) {
        OrderProd op = new OrderProd();
        op.setProdQty(qty);
        op.setSalePrice(sale);
        op.setNvPrice(nv);
        op.setSupplyPrice(supply);
        op.setPromotFeeAmt(0);
        return op;
    }

    @Test
    void 마스터_공급가와_원가는_단가라서_수량을_곱해_채운다() {
        OrderProd op = line(4, 10400, 0, null);   // 수량 4, 판매가 라인 합계 10,400

        ProdMatchService.backfillLine(op, prod(2688, 4100, 10));

        assertThat(op.getSupplyPrice()).isEqualTo(10752);   // 2688 × 4
        assertThat(op.getBuyPrice()).isEqualTo(16400);      // 4100 × 4
        assertThat(op.getProdMargin()).isEqualTo(10400 - 16400);
        assertThat(op.getChnlMargin()).isEqualTo(10400 - 10752);
        assertThat(op.getNetMargin()).isEqualTo(10752 - 16400);
    }

    @Test
    void 채널이_준_공급가는_이미_합계이므로_그대로_쓴다() {
        OrderProd op = line(4, 10400, 0, 9000);

        ProdMatchService.backfillLine(op, prod(2688, 4100, 10));

        assertThat(op.getSupplyPrice()).isEqualTo(9000);
        assertThat(op.getBuyPrice()).isEqualTo(16400);
    }

    @Test
    void 수량이_1이거나_없으면_마스터_단가_그대로다() {
        OrderProd one = line(1, 2600, 0, null);
        ProdMatchService.backfillLine(one, prod(2688, 4100, 10));
        assertThat(one.getSupplyPrice()).isEqualTo(2688);
        assertThat(one.getBuyPrice()).isEqualTo(4100);

        OrderProd none = new OrderProd();
        none.setSalePrice(2600);
        ProdMatchService.backfillLine(none, prod(2688, 4100, 10));
        assertThat(none.getBuyPrice()).isEqualTo(4100);
    }
}
