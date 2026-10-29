package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.order.model.OrderProd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * CheckoutService의 결제 정합성 검증 로직(코드리뷰에서 하드닝한 부분)을
 * 실제 DB/토스 서버 없이 빠르게 검증하는 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final String CHNL_CODE = "1208674775Z2";

    @Mock private OrderNoSeqMapper orderNoSeqMapper;
    @Mock private OrderService orderService;
    @Mock private OrderProdService orderProdService;
    @Mock private OrderPayService orderPayService;
    @Mock private OrderPayLogTossMapper orderPayLogTossMapper;
    @Mock private StockLedgerService stockLedgerService;

    @InjectMocks private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(checkoutService, "shopChnlCode", CHNL_CODE);

        // OrderNoSeqMapper.insert(seq)는 seq 객체에 채번된 값을 채워 넣는(mutate) void 메서드.
        // 금액 검증 실패 테스트 등 이 stub까지 도달하지 않는 케이스도 있어 lenient 처리.
        lenient().doAnswer(inv -> {
            OrderNoSeq seq = inv.getArgument(0);
            seq.setSeq(999001);
            return null;
        }).when(orderNoSeqMapper).insert(any());
    }

    private CheckoutRequest baseRequest(String payMethod, int totalAmt) {
        CheckoutRequest req = new CheckoutRequest();
        req.setPaymentKey("test-payment-key");
        req.setOrderId("SW-TEST-ORDER");
        req.setAmount((long) totalAmt);
        req.setCustId(2);
        req.setOrderName("테스트주문자");
        req.setRecverName("테스트수취인");
        req.setRecverAddr("서울시 어딘가");
        req.setPayMethod(payMethod);
        req.setTotalAmt(totalAmt);
        req.setRegistId("rohhakdong");
        req.setRegistName("노학동");

        CheckoutProdItem item = new CheckoutProdItem();
        item.setProdCode("1000000342");
        item.setProdName("테스트상품");
        item.setItemCode("20000");
        item.setItemName("선택사항없음");
        item.setProdQty(1);
        item.setSalePrice(totalAmt);
        item.setShopCode("2068542226A1");
        req.setOrderProds(List.of(item));

        return req;
    }

    private Map<String, Object> tossResp(String status, long totalAmount) {
        return Map.of("status", status, "totalAmount", totalAmount, "method", "카드");
    }

    @Test
    void 결제금액이_토스승인금액과_다르면_거부한다() {
        CheckoutRequest req = baseRequest("card", 26900);
        Map<String, Object> resp = tossResp("DONE", 10000L); // 토스 승인액(10000) != 클라이언트 값(26900)

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkoutService.checkout(req, resp));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verify(orderNoSeqMapper, never()).insert(any());
        verify(orderService, never()).insert(any());
    }

    @Test
    void 토스응답에_totalAmount가_없으면_거부한다() {
        CheckoutRequest req = baseRequest("card", 26900);
        Map<String, Object> resp = Map.of("status", "DONE"); // totalAmount 누락

        assertThrows(ResponseStatusException.class, () -> checkoutService.checkout(req, resp));
        verify(orderService, never()).insert(any());
    }

    @Test
    void 토스상태가_DONE이면_지불완료로_저장한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);
        Map<String, Object> resp = tossResp("DONE", 26900L);

        checkoutService.checkout(req, resp);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getOrderState()).isEqualTo("지불완료");
        assertThat(saved.getPayCmpletDate()).isNotNull();
        assertThat(saved.getChnlCode()).isEqualTo(CHNL_CODE);
    }

    @Test
    void 가상계좌_입금대기_상태면_지불완료로_찍지_않는다() throws Exception {
        CheckoutRequest req = baseRequest("vbank", 26900);
        Map<String, Object> resp = tossResp("WAITING_FOR_DEPOSIT", 26900L);

        checkoutService.checkout(req, resp);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getOrderState()).isEqualTo("주문접수");
        assertThat(saved.getPayCmpletDate()).isNull();

        ArgumentCaptor<OrderProd> prodCaptor = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(prodCaptor.capture());
        assertThat(prodCaptor.getValue().getOrderState()).isEqualTo("주문접수");
    }

    @Test
    void KCP_가상계좌는_계좌와_입금예정정보를_결제건에_저장하고_주문접수로_둔다() throws Exception {
        CheckoutRequest req = baseRequest("vbank", 26900);
        com.shopwizard.payment.model.PaymentResult r = new com.shopwizard.payment.model.PaymentResult();
        r.setPgCode("KCP"); r.setTradeNo("26931077341432"); r.setOrderCode("SW-TEST-ORDER"); r.setAmount(26900L);
        r.setStatus(com.shopwizard.payment.model.PaymentResult.STATUS_WAITING_DEPOSIT); r.setMethod("VBANK");
        r.setReciptBank("신한은행"); r.setAccount("T0123456789"); r.setDueAt("2026-11-01 10:00:00"); r.setExpectName("테스트주문자");

        checkoutService.checkout(req, r);

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(order.capture());
        assertThat(order.getValue().getOrderState()).isEqualTo("주문접수");
        assertThat(order.getValue().getPayCmpletDate()).isNull();

        ArgumentCaptor<OrderPay> pay = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(pay.capture());
        OrderPay saved = pay.getValue();
        assertThat(saved.getPayType()).isEqualTo("001000000000");
        assertThat(saved.getReciptBank()).isEqualTo("신한은행");
        assertThat(saved.getReciptNo()).isEqualTo("T0123456789");          // ReciptAccnt(저장은 reciptNo 로 넘기는 관례)
        assertThat(saved.getPayExpectAmt()).isEqualByComparingTo("26900");
        assertThat(saved.getPayExpectDate()).isEqualTo("2026-11-01 10:00:00");
        assertThat(saved.getPayExpectName()).isEqualTo("테스트주문자");
        assertThat(saved.getPgCode()).isEqualTo("KCP");
        assertThat(saved.getPgTradeNo()).isEqualTo("26931077341432");
    }

    @Test
    void 카드_결제건에는_가상계좌_정보를_넣지_않는다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);

        checkoutService.checkout(req, tossResp("DONE", 26900L));

        ArgumentCaptor<OrderPay> pay = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(pay.capture());
        assertThat(pay.getValue().getReciptNo()).isNull();
        assertThat(pay.getValue().getPayExpectDate()).isNull();
    }

    @Test
    void 이미_저장된_가상계좌_주문의_입금_안내를_다시_돌려준다() {
        OrderPay p = new OrderPay();
        p.setOrderNo(7); p.setReciptBank("국민은행"); p.setReciptAccnt("T999"); p.setPayAmt(5000);
        p.setPayExpectDate("2026-11-02 00:00:00"); p.setPayExpectName("홍길동");
        org.mockito.Mockito.when(orderPayService.select(any())).thenReturn(p);

        Map<String, Object> info = checkoutService.findVbankInfo(7);

        assertThat(info).containsEntry("bank", "국민은행").containsEntry("account", "T999").containsEntry("amount", 5000L)
                .containsEntry("dueAt", "2026-11-02 00:00:00").containsEntry("expectName", "홍길동");
        // 카드 결제건은 계좌가 없어 안내가 없다.
        p.setReciptAccnt(null);
        assertThat(checkoutService.findVbankInfo(7)).isNull();
    }

    @Test
    void PayNo는_항상_1로_채번한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);
        checkoutService.checkout(req, tossResp("DONE", 26900L));

        ArgumentCaptor<OrderPay> payCaptor = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(payCaptor.capture());
        assertThat(payCaptor.getValue().getPayNo()).isEqualTo(1);
    }

    @Test
    void 결제수단별로_PayType_코드값을_매핑한다() throws Exception {
        checkoutService.checkout(baseRequest("card", 100), tossResp("DONE", 100L));
        checkoutService.checkout(baseRequest("trans", 100), tossResp("DONE", 100L));
        checkoutService.checkout(baseRequest("vbank", 100), tossResp("WAITING_FOR_DEPOSIT", 100L));

        ArgumentCaptor<OrderPay> payCaptor = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService, times(3)).insert(payCaptor.capture());
        List<OrderPay> pays = payCaptor.getAllValues();
        assertThat(pays.get(0).getPayType()).isEqualTo("100000000000"); // 신용카드
        assertThat(pays.get(1).getPayType()).isEqualTo("010000000000"); // 계좌이체
        assertThat(pays.get(2).getPayType()).isEqualTo("001000000000"); // 가상계좌
    }

    @Test
    void 결제금액은_토스승인금액을_저장하고_생성된_주문번호를_반환한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);

        int orderNo = checkoutService.checkout(req, tossResp("DONE", 26900L));

        assertThat(orderNo).isEqualTo(999001);

        ArgumentCaptor<OrderPay> payCaptor = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(payCaptor.capture());
        assertThat(payCaptor.getValue().getPayAmt()).isEqualTo(26900);

        ArgumentCaptor<OrderPayLogToss> logCaptor = ArgumentCaptor.forClass(OrderPayLogToss.class);
        verify(orderPayLogTossMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getOrderNo()).isEqualTo(999001);
    }

    @Test
    void custId가_없으면_비회원_CustId_0으로_저장한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);
        req.setCustId(null);
        req.setRegistId("");
        req.setRegistName("");

        checkoutService.checkout(req, tossResp("DONE", 26900L));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCustId()).isEqualTo(0);
    }

    @Test
    void 비회원인데_registId가_비어있으면_GUEST로_기본값을_채운다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900);
        req.setCustId(0);
        req.setRegistId("");
        req.setRegistName("");

        checkoutService.checkout(req, tossResp("DONE", 26900L));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getRegistId()).isEqualTo("GUEST");
        assertThat(saved.getLoginId()).isEqualTo("GUEST");
        assertThat(saved.getRegistName()).isEqualTo(req.getOrderName()); // 주문자명으로 대체
    }

    @Test
    void 회원_주문은_로그인_계정정보를_그대로_사용한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26900); // custId=2, registId="rohhakdong"

        checkoutService.checkout(req, tossResp("DONE", 26900L));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getCustId()).isEqualTo(2);
        assertThat(saved.getRegistId()).isEqualTo("rohhakdong");
        assertThat(saved.getRegistName()).isEqualTo("노학동");
    }

    @Test
    void 주문상품_각_라인마다_순번을_1부터_채번한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 30000);
        req.getOrderProds().get(0).setSalePrice(10000);   // 라인 합계(10000 + 10000×2)가 승인액(30000)을 넘지 않게
        CheckoutProdItem second = new CheckoutProdItem();
        second.setProdCode("1000000999");
        second.setProdName("두번째상품");
        second.setItemCode("20001");
        second.setItemName("옵션없음");
        second.setProdQty(2);
        second.setSalePrice(10000);
        second.setShopCode("2068542226A1");
        req.setOrderProds(List.of(req.getOrderProds().get(0), second));

        checkoutService.checkout(req, tossResp("DONE", 30000L));

        ArgumentCaptor<OrderProd> prodCaptor = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService, times(2)).insert(prodCaptor.capture());
        List<OrderProd> lines = prodCaptor.getAllValues();
        assertThat(lines.get(0).getOrderProdNo()).isEqualTo(1);
        assertThat(lines.get(1).getOrderProdNo()).isEqualTo(2);
        assertThat(lines.get(1).getReciptAmt()).isEqualTo(20000); // 10000 * 2개
    }

    @Test
    void 옵션_추가금은_NvPrice_로_저장하고_영수금액은_판매가_플러스_옵션가_곱하기_수량이다() throws Exception {
        CheckoutRequest req = baseRequest("card", 26000);
        CheckoutProdItem item = req.getOrderProds().get(0);
        item.setSalePrice(10000);
        item.setOptionPrice(3000);
        item.setListPrice(12000);
        item.setProdQty(2);          // (10000 + 3000) × 2 = 26000

        checkoutService.checkout(req, tossResp("DONE", 26000L));

        ArgumentCaptor<OrderProd> cap = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(cap.capture());
        OrderProd op = cap.getValue();
        // 수량 2: 라인 금액은 모두 수량이 곱해진 합계로 저장한다(다른 채널과 같은 규칙 — 주문관리·마진·정산이 그대로 합산)
        assertThat(op.getSalePrice()).isEqualTo(20000);
        assertThat(op.getNvPrice()).isEqualTo(6000);
        assertThat(op.getListPrice()).isEqualTo(24000);
        assertThat(op.getReciptAmt()).isEqualTo(26000);
        assertThat(op.getSalePrice() + op.getNvPrice()).isEqualTo(op.getReciptAmt());
    }

    @Test
    void 배송비가_있어_승인액이_라인합계보다_커도_저장된다() throws Exception {
        CheckoutRequest req = baseRequest("card", 12500);
        req.getOrderProds().get(0).setSalePrice(10000);      // 라인 10000 + 배송비 2500 = 승인 12500

        int orderNo = checkoutService.checkout(req, tossResp("DONE", 12500L));

        assertThat(orderNo).isEqualTo(999001);
    }

    @Test
    void 주문_라인_합계가_토스_승인액을_넘으면_저장하지_않는다() {
        CheckoutRequest req = baseRequest("card", 1000);
        req.getOrderProds().get(0).setSalePrice(50000);      // 1000원만 승인됐는데 라인은 5만원

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkoutService.checkout(req, tossResp("DONE", 1000L)));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verify(orderNoSeqMapper, never()).insert(any());
        verify(orderService, never()).insert(any());
    }

    @Test
    void 주문_라인마다_재고를_차감한다_상품코드_순으로() throws Exception {
        CheckoutRequest req = baseRequest("card", 30000);
        req.getOrderProds().get(0).setProdCode("1000000500");
        req.getOrderProds().get(0).setSalePrice(10000);
        CheckoutProdItem second = new CheckoutProdItem();
        second.setProdCode("1000000100");     // 먼저 담긴 라인보다 코드가 앞 — 차감은 코드 순
        second.setProdName("두번째상품");
        second.setItemCode("20000");
        second.setProdQty(2);
        second.setSalePrice(10000);
        req.setOrderProds(List.of(req.getOrderProds().get(0), second));

        checkoutService.checkout(req, tossResp("DONE", 30000L));

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(stockLedgerService);
        order.verify(stockLedgerService).deductForOrder(999001, 2, "1000000100", 2, "rohhakdong");
        order.verify(stockLedgerService).deductForOrder(999001, 1, "1000000500", 1, "rohhakdong");
    }

    @Test
    void 재고_차감이_실패하면_결제_정보를_저장하지_않고_예외를_그대로_올린다() {
        CheckoutRequest req = baseRequest("card", 26900);
        org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "재고가 부족합니다."))
                .when(stockLedgerService).deductForOrder(any(), any(), any(), any(), any());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> checkoutService.checkout(req, tossResp("DONE", 26900L)));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(orderPayService, never()).insert(any());
        verify(orderPayLogTossMapper, never()).insert(any());
    }
}
