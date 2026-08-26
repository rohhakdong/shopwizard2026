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
    void 주문상품_각_라인마다_순번을_1부터_채번한다() throws Exception {
        CheckoutRequest req = baseRequest("card", 30000);
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
}
