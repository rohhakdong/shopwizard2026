package com.shopwizard.order.service;

import com.shopwizard.company.model.Chnl;
import com.shopwizard.company.service.ChnlService;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.ManualOrderRequest;
import com.shopwizard.order.model.ManualOrderResult;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 주문관리 "주문등록"(수기 주문) — 지불완료·수기 결제·수량 곱한 라인 금액·원장 우선 재고 차감·검증. */
@ExtendWith(MockitoExtension.class)
class ManualOrderServiceTest {

    @Mock private OrderNoSeqMapper orderNoSeqMapper;
    @Mock private OrderService orderService;
    @Mock private OrderProdService orderProdService;
    @Mock private OrderPayService orderPayService;
    @Mock private StockLedgerService stockLedgerService;
    @Mock private CheckoutPricingService pricingService;
    @Mock private ProdService prodService;
    @Mock private ChnlService chnlService;
    @Mock private OrderProdMapper orderProdMapper;

    @InjectMocks private ManualOrderService service;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(inv -> {
            OrderNoSeq seq = inv.getArgument(0);
            seq.setSeq(777001);
            return null;
        }).when(orderNoSeqMapper).insert(any());
        lenient().when(chnlService.get(any())).thenReturn(new Chnl());
    }

    // ── 픽스처 ──────────────────────────────────────────────────────────

    private static ManualOrderRequest.Line line(String prodCode, String itemCode, int qty, Integer sale, Integer opt) {
        ManualOrderRequest.Line l = new ManualOrderRequest.Line();
        l.setProdCode(prodCode);
        l.setItemCode(itemCode);
        l.setProdQty(qty);
        l.setSalePrice(sale);
        l.setOptionPrice(opt);
        return l;
    }

    private static ManualOrderRequest request(ManualOrderRequest.Line... lines) {
        ManualOrderRequest r = new ManualOrderRequest();
        r.setChnlCode("CH1");
        r.setOrderName("홍길동");
        r.setOrderMobileNo("010-1111-2222");
        r.setRecverName("김철수");
        r.setRecverMobileNo("010-3333-4444");
        r.setRecverAddr1("서울시 중구 1");
        r.setLines(List.of(lines));
        r.setRegistId("admin");
        r.setRegistName("관리자");
        return r;
    }

    private static PricedLine priced(String prodCode, String itemCode, String itemName, int qty, int sale, int opt, int list) {
        PricedLine p = new PricedLine();
        p.setProdCode(prodCode);
        p.setProdName("상품" + prodCode);
        p.setShopCode("SH1");
        p.setItemCode(itemCode);
        p.setItemName(itemName);
        p.setProdQty(qty);
        p.setListPrice(list);
        p.setSalePrice(sale);
        p.setOptionPrice(opt);
        p.setUnitPrice(sale + opt);
        p.setLineAmt((long) (sale + opt) * qty);
        return p;
    }

    private void givenPricing(PricedLine... lines) {
        PricedOrder po = new PricedOrder();
        po.setLines(List.of(lines));
        when(pricingService.price(any())).thenReturn(po);
    }

    private static Prod prod(String code, int supply, int buy, String deliFeeType) {
        Prod p = new Prod();
        p.setProdCode(code);
        p.setShopCode("SH1");
        p.setShopName("상점1");
        p.setSupplyPrice(supply);
        p.setBuyPrice(buy);
        p.setDeliFeeType(deliFeeType);
        return p;
    }

    private void givenProds(Prod... prods) {
        for (Prod p : prods) {
            when(prodService.selectList(Map.of("pProdCode", p.getProdCode()))).thenReturn(List.of(p));
        }
    }

    private void expectBad(ManualOrderRequest req, int status, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(req));
        assertThat(ex.getStatusCode().value()).isEqualTo(status);
        assertThat(ex.getReason()).contains(messagePart);
        verify(orderNoSeqMapper, never()).insert(any());
        verify(orderService, never()).insert(any());
        verify(orderProdService, never()).insert(any());
        verify(orderPayService, never()).insert(any());
        verify(stockLedgerService, never()).deductForOrder(any(), any(), any(), any(), any());
    }

    // ── 정상 생성 ───────────────────────────────────────────────────────

    @Test
    void 지불완료_주문과_수기_결제가_저장되고_라인_금액은_수량을_곱한_합계다() {
        givenPricing(priced("P1", "20001", "빨강", 2, 10000, 500, 12000));
        givenProds(prod("P1", 7000, 5000, "선불"));
        ManualOrderRequest req = request(line("P1", "20001", 2, null, null));
        req.setChnlOrderNo("OM-1234");
        req.setDeliMemo("문 앞");

        ManualOrderResult res = service.create(req);

        assertThat(res.orderNo()).isEqualTo(777001);
        assertThat(res.lineCount()).isEqualTo(1);
        assertThat(res.totalAmt()).isEqualTo(21000);   // (10000+500)*2

        ArgumentCaptor<Order> oc = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(oc.capture());
        Order o = oc.getValue();
        assertThat(o.getOrderNo()).isEqualTo(777001);
        assertThat(o.getOrderState()).isEqualTo("지불완료");
        assertThat(o.getCustId()).isZero();
        assertThat(o.getChnlCode()).isEqualTo("CH1");
        assertThat(o.getOrderReciptDate()).isNotBlank();   // 비우면 날짜로 검색하는 화면에서 주문이 빠진다
        assertThat(o.getPayCmpletDate()).isNotBlank();
        assertThat(o.getRecverAddr2()).isEqualTo("");      // NULL 이면 출고지시 주소 CONCAT 이 NULL
        assertThat(o.getDeliMemo()).isEqualTo("문 앞");
        assertThat(o.getRegistId()).isEqualTo("admin");

        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        OrderProd op = pc.getValue();
        assertThat(op.getOrderNo()).isEqualTo(777001);
        assertThat(op.getOrderProdNo()).isEqualTo(1);
        assertThat(op.getOrderState()).isEqualTo("지불완료");
        assertThat(op.getItemCode()).isEqualTo("20001");
        assertThat(op.getItemName()).isEqualTo("빨강");
        assertThat(op.getChnlOrderNo()).isEqualTo("OM-1234");
        assertThat(op.getListPrice()).isEqualTo(24000);
        assertThat(op.getSalePrice()).isEqualTo(20000);
        assertThat(op.getNvPrice()).isEqualTo(1000);
        assertThat(op.getReciptAmt()).isEqualTo(21000);
        // 공급가·매입가는 상품 마스터 단가 × 수량, 마진은 그 위에서 계산
        assertThat(op.getSupplyPrice()).isEqualTo(14000);
        assertThat(op.getBuyPrice()).isEqualTo(10000);
        assertThat(op.getProdMargin()).isEqualTo(11000);   // 20000+1000-10000
        assertThat(op.getChnlMargin()).isEqualTo(7000);    // 20000+1000-14000
        assertThat(op.getNetMargin()).isEqualTo(4000);     // 14000-10000
        assertThat(op.getShopCode()).isEqualTo("SH1");
        assertThat(op.getDeliFeeType()).isEqualTo("선불");
        assertThat(op.getDeliFeeAmt()).isZero();

        ArgumentCaptor<OrderPay> payc = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(payc.capture());
        OrderPay pay = payc.getValue();
        assertThat(pay.getPayNo()).isEqualTo(1);               // 관리자 조회가 PayNo=1 을 대표 결제건으로 본다
        assertThat(pay.getPayType()).isEqualTo("M00000000000"); // 수기
        assertThat(pay.getPgCode()).isEqualTo("MANUAL");
        assertThat(pay.getPayAmt()).isEqualTo(21000);
        assertThat(pay.getPgOrderNo()).isEqualTo("OM-1234");

        verify(stockLedgerService).deductForOrder(777001, 1, "P1", 2, "admin");
    }

    @Test
    void 화면에서_고친_판매가와_옵션가가_상품_현재가보다_우선한다() {
        givenPricing(priced("P1", null, null, 1, 10000, 500, 10000));
        givenProds(prod("P1", 0, 0, null));

        ManualOrderResult res = service.create(request(line("P1", null, 1, 8000, 0)));

        assertThat(res.totalAmt()).isEqualTo(8000);
        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        assertThat(pc.getValue().getSalePrice()).isEqualTo(8000);
        assertThat(pc.getValue().getNvPrice()).isZero();
    }

    @Test
    void 판매가를_0원으로_고치는_것은_허용한다() {
        givenPricing(priced("P1", null, null, 1, 10000, 0, 10000));
        givenProds(prod("P1", 0, 0, null));

        assertThat(service.create(request(line("P1", null, 1, 0, 0))).totalAmt()).isZero();
    }

    @Test
    void 채널주문번호가_없으면_결제_참조는_주문번호다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));

        service.create(request(line("P1", null, 1, null, null)));

        ArgumentCaptor<OrderPay> payc = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(payc.capture());
        assertThat(payc.getValue().getPgOrderNo()).isEqualTo("777001");
        verify(orderProdMapper, never()).countByChnlOrderNo(anyString(), anyString());
    }

    @Test
    void 여러_줄은_번호를_1부터_붙이고_재고는_상품코드_순으로_차감한다() {
        givenPricing(priced("PB", null, null, 1, 1000, 0, 1000), priced("PA", null, null, 3, 2000, 0, 2000));
        givenProds(prod("PA", 0, 0, null), prod("PB", 0, 0, null));

        ManualOrderResult res = service.create(request(line("PB", null, 1, null, null), line("PA", null, 3, null, null)));

        assertThat(res.lineCount()).isEqualTo(2);
        assertThat(res.totalAmt()).isEqualTo(7000);
        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService, org.mockito.Mockito.times(2)).insert(pc.capture());
        assertThat(pc.getAllValues()).extracting(OrderProd::getOrderProdNo).containsExactly(1, 2);
        assertThat(pc.getAllValues()).extracting(OrderProd::getProdCode).containsExactly("PB", "PA");

        InOrder order = inOrder(stockLedgerService);
        order.verify(stockLedgerService).deductForOrder(777001, 2, "PA", 3, "admin");
        order.verify(stockLedgerService).deductForOrder(777001, 1, "PB", 1, "admin");
    }

    @Test
    void 주문일자는_날짜만_받아_그날로_저장한다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));
        ManualOrderRequest req = request(line("P1", null, 1, null, null));
        req.setOrderReciptDate("2026-11-03");

        service.create(req);

        ArgumentCaptor<Order> oc = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(oc.capture());
        assertThat(oc.getValue().getOrderReciptDate()).startsWith("2026-11-03 ");
    }

    @Test
    void 가격_조회는_상품코드가_정확히_같은_상품을_쓴다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        Prod other = prod("P1-OTHER", 9999, 9999, null);   // 상점상품코드가 P1 인 다른 상품이 먼저 걸려도
        Prod mine = prod("P1", 100, 50, null);
        when(prodService.selectList(Map.of("pProdCode", "P1"))).thenReturn(List.of(other, mine));

        service.create(request(line("P1", null, 1, null, null)));

        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        assertThat(pc.getValue().getSupplyPrice()).isEqualTo(100);
    }

    // ── 검증(DB 에 쓰기 전에 거절) ──────────────────────────────────────

    @Test
    void 채널을_고르지_않으면_거절한다() {
        ManualOrderRequest req = request(line("P1", null, 1, null, null));
        req.setChnlCode(" ");
        expectBad(req, 400, "채널");
    }

    @Test
    void 없는_채널이면_거절한다() {
        when(chnlService.get(any())).thenReturn(null);
        expectBad(request(line("P1", null, 1, null, null)), 400, "존재하지 않는 채널");
    }

    @Test
    void 필수_입력이_비면_거절한다() {
        List<Consumer<ManualOrderRequest>> breakers = List.of(
                r -> r.setOrderName(""),
                r -> r.setOrderMobileNo(null),
                r -> r.setRecverName(" "),
                r -> r.setRecverMobileNo(""),
                r -> r.setRecverAddr1(null));
        List<String> messages = List.of("주문자 이름", "주문자 연락처", "수취인 이름", "수취인 연락처", "수취인 주소");
        for (int i = 0; i < breakers.size(); i++) {
            ManualOrderRequest req = request(line("P1", null, 1, null, null));
            breakers.get(i).accept(req);
            expectBad(req, 400, messages.get(i));
        }
    }

    @Test
    void 연락처는_휴대폰_또는_전화_중_하나면_된다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));
        ManualOrderRequest req = request(line("P1", null, 1, null, null));
        req.setOrderMobileNo(null);
        req.setOrderPhoneNo("02-123-4567");
        req.setRecverMobileNo(null);
        req.setRecverPhoneNo("02-765-4321");

        assertThat(service.create(req).orderNo()).isEqualTo(777001);
    }

    @Test
    void 상품이_없으면_거절한다() {
        ManualOrderRequest req = request();
        expectBad(req, 400, "주문 상품");
        req.setLines(null);
        expectBad(req, 400, "주문 상품");
    }

    @Test
    void 이미_등록된_채널주문번호면_409() {
        ManualOrderRequest req = request(line("P1", null, 1, null, null));
        req.setChnlOrderNo("OM-1");
        when(orderProdMapper.countByChnlOrderNo("CH1", "OM-1")).thenReturn(1);

        expectBad(req, 409, "이미 등록된 채널주문번호");
    }

    @Test
    void 주문일자_형식이_틀리면_거절한다() {
        ManualOrderRequest req = request(line("P1", null, 1, null, null));
        req.setOrderReciptDate("11/03/2026");
        expectBad(req, 400, "주문일자");
    }

    @Test
    void 음수_가격은_거절한다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));   // 가격 검사가 상품 조회보다 먼저라 상품은 stub 하지 않는다
        expectBad(request(line("P1", null, 1, -1, null)), 400, "가격");
        expectBad(request(line("P1", null, 1, null, -5)), 400, "가격");
    }

    @Test
    void 금액이_int_범위를_넘으면_거절한다() {
        givenPricing(priced("P1", null, null, 999, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));
        expectBad(request(line("P1", null, 999, Integer.MAX_VALUE, null)), 400, "너무 큽니다");
    }

    @Test
    void 상품_옵션_재고_검사에서_걸리면_그_메시지로_거절하고_아무것도_저장하지_않는다() {
        when(pricingService.price(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "옵션을 선택해 주세요: 상품"));
        expectBad(request(line("P1", null, 1, null, null)), 400, "옵션을 선택해 주세요");
    }

    @Test
    void 판매_상품을_찾을_수_없으면_거절한다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        when(prodService.selectList(any())).thenReturn(List.of());
        expectBad(request(line("P1", null, 1, null, null)), 400, "존재하지 않는 상품");
    }

    @Test
    void 가격검사에는_상품코드_옵션코드_수량만_넘긴다() {
        givenPricing(priced("P1", "20001", "빨강", 2, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));

        service.create(request(line("P1", "20001", 2, 1, 1)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CheckoutProdItem>> cap = ArgumentCaptor.forClass(List.class);
        verify(pricingService).price(cap.capture());
        CheckoutProdItem it = cap.getValue().get(0);
        assertThat(it.getProdCode()).isEqualTo("P1");
        assertThat(it.getItemCode()).isEqualTo("20001");
        assertThat(it.getProdQty()).isEqualTo(2);
        assertThat(it.getSalePrice()).isNull();   // 클라이언트 가격은 검증 입력이 아니다
    }

    // ── 재고 부족 ───────────────────────────────────────────────────────

    @Test
    void 재고가_부족하면_예외가_그대로_나가고_결제_행은_저장하지_않는다() {
        givenPricing(priced("P1", null, null, 1, 1000, 0, 1000));
        givenProds(prod("P1", 0, 0, null));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고가 부족합니다"))
                .when(stockLedgerService).deductForOrder(anyInt(), anyInt(), eq("P1"), anyInt(), anyString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(request(line("P1", null, 1, null, null))));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(orderPayService, never()).insert(any());   // 트랜잭션이 롤백되므로 주문·줄도 남지 않는다
    }
}
