package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.service.PaymentGateway;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import com.shopwizard.payment.service.TossPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundConfirmServiceTest {

    private static final int ORDER_NO = 3001;
    private static final int PROD_NO = 2;
    private static final String PAYMENT_KEY = "pay-key-1";

    @Mock private OrderMapper orderMapper;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private OrderPayMapper orderPayMapper;
    @Mock private OrderPayLogTossMapper orderPayLogTossMapper;
    @Mock private TossPaymentService tossPaymentService;
    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private PaymentGateway kcp;
    @InjectMocks private RefundConfirmService service;

    private OrderProd line(String state, Integer reciptAmt, Integer salePrice, Integer qty) {
        OrderProd op = new OrderProd();
        op.setOrderNo(ORDER_NO);
        op.setOrderProdNo(PROD_NO);
        op.setOrderState(state);
        op.setReciptAmt(reciptAmt);
        op.setSalePrice(salePrice);
        op.setProdQty(qty);
        return op;
    }

    private OrderPay tossPay(String payType) {
        OrderPay pay = new OrderPay();
        pay.setOrderNo(ORDER_NO);
        pay.setPgCode("TOSS");
        pay.setPgTradeNo(PAYMENT_KEY);
        pay.setPayType(payType);
        return pay;
    }

    private void givenLine(OrderProd op) {
        when(lineLock.lockHeaderAndLine(any(), any())).thenReturn(op);
    }

    @Test
    void 환불확정_가능한_상태가_아니면_아무것도_바꾸지_않는다() throws Exception {
        givenLine(line("배송완료", 1000, 1000, 1));

        String err = service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자");

        assertThat(err).contains("환불확정 가능한 상태가 아닙니다").contains("배송완료");
        verifyNoInteractions(orderPayMapper, tossPaymentService);
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 주문라인이_없으면_실패() throws Exception {
        when(lineLock.lockHeaderAndLine(any(), any())).thenReturn(null);

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자")).contains("찾을 수 없습니다");
    }

    @Test
    void 토스_결제가_아닌_주문_마켓주문은_PG호출없이_상태만_바꾼다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(null);
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자")).isNull();

        verifyNoInteractions(tossPaymentService, orderPayLogTossMapper);
        verify(orderProdMapper).updateOrderState(any());
        verify(orderMapper).updateOrderState(any());   // 모든 라인이 환불완료 → 헤더 승격
    }

    @Test
    void 토스_결제는_라인금액으로_부분취소를_호출하고_멱등키와_로그를_남긴다() throws Exception {
        givenLine(line("반품완료", 12000, 4000, 3));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE"));
        when(tossPaymentService.cancelPartial(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("status", "PARTIAL_CANCELED", "paymentKey", PAYMENT_KEY, "totalAmount", 20000L));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);   // 다른 라인이 남아 있어 헤더 유지

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자")).isNull();

        // ReciptAmt(12000)가 우선, 멱등키는 라인 단위로 고정 → 재시도해도 이중 환불이 안 된다
        verify(tossPaymentService).cancelPartial(PAYMENT_KEY, "환불확정", 12000L, "refund-" + ORDER_NO + "-" + PROD_NO);
        verify(orderPayLogTossMapper).insert(any());
        verify(orderMapper, never()).updateOrderState(any());
    }

    @Test
    void DB상태를_먼저_바꾼_뒤_토스를_호출한다_웹훅이_덮어쓰지_못하게_행잠금을_먼저_잡는다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE"));
        when(tossPaymentService.cancelPartial(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("status", "CANCELED"));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자");

        InOrder order = inOrder(orderProdMapper, tossPaymentService);
        order.verify(tossPaymentService).getPayment(PAYMENT_KEY);
        order.verify(orderProdMapper).updateOrderState(any());
        order.verify(tossPaymentService).cancelPartial(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void ReciptAmt가_없으면_판매가x수량으로_계산한다() throws Exception {
        givenLine(line("주문취소", null, 3500, 2));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "PARTIAL_CANCELED"));
        when(tossPaymentService.cancelPartial(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("status", "PARTIAL_CANCELED"));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자");

        verify(tossPaymentService).cancelPartial(eq(PAYMENT_KEY), anyString(), eq(7000L), anyString());
    }

    @Test
    void skipPg이면_토스_결제여도_호출하지_않고_상태만_바꾼다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, true, "admin", "관리자")).isNull();

        verifyNoInteractions(orderPayMapper, tossPaymentService, orderPayLogTossMapper);
        verify(orderProdMapper).updateOrderState(any());
    }

    @Test
    void 가상계좌는_자동취소하지_않고_DB도_바꾸지_않는다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("001000000000"));

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("가상계좌").hasMessageContaining("토스 호출 생략");

        verifyNoInteractions(tossPaymentService);
        verify(orderProdMapper, never()).updateOrderState(any());   // 검증 단계에서 막혀 DB 변경 전에 끝난다
    }

    @Test
    void 환불금액이_0원이면_실패한다() throws Exception {
        givenLine(line("주문취소", 0, 0, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("환불 금액");
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 이미_전액_취소된_결제는_토스를_호출하지_않고_상태만_반영한다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "CANCELED"));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자")).isNull();

        verify(tossPaymentService, never()).cancelPartial(anyString(), anyString(), anyLong(), anyString());
        verify(orderProdMapper).updateOrderState(any());
    }

    @Test
    void 대기중_만료_등_취소할_수_없는_결제상태면_실패한다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "WAITING_FOR_DEPOSIT"));

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("WAITING_FOR_DEPOSIT");
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 토스_취소가_실패하면_예외로_전파돼_트랜잭션이_롤백된다() throws Exception {
        givenLine(line("주문취소", 5000, 5000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(tossPay("100000000000"));
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE"));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);
        when(tossPaymentService.cancelPartial(anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "취소 가능한 잔액이 없습니다"));

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("토스 결제 취소 실패").hasMessageContaining("취소 가능한 잔액이 없습니다");

        // 예외가 REQUIRES_NEW 경계 밖으로 나가므로 앞서 한 DB 변경은 함께 롤백된다. 로그는 남지 않는다.
        verify(orderPayLogTossMapper, never()).insert(any());
        ArgumentCaptor<Map<String, Object>> upd = ArgumentCaptor.forClass(Map.class);
        verify(orderProdMapper, times(1)).updateOrderState(upd.capture());
        assertThat(upd.getValue()).containsEntry("orderState", "환불완료");
    }

    // ── KCP (토스 외 PG: 게이트웨이 + 잔액 대조) ─────────────────────────

    private OrderPay kcpPay(String payType, int payAmt) {
        OrderPay pay = new OrderPay();
        pay.setOrderNo(ORDER_NO);
        pay.setPgCode("KCP");
        pay.setPgTradeNo("KCP-TNO-1");
        pay.setPayType(payType);
        pay.setPayAmt(payAmt);
        return pay;
    }

    @Test
    void KCP_결제는_결제금액에서_이미_환불한_금액을_뺀_직전_잔액과_함께_게이트웨이로_취소한다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("100000000000", 10000));
        when(gatewayRegistry.forExistingPayment("KCP")).thenReturn(kcp);
        when(orderProdMapper.sumRefundedAmount(ORDER_NO)).thenReturn(2000L);   // 앞서 2,000원 환불됨 → 직전 잔액 8,000
        when(kcp.cancel(any())).thenReturn(new CancelResult());
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThat(service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자")).isNull();

        ArgumentCaptor<CancelRequest> cap = ArgumentCaptor.forClass(CancelRequest.class);
        verify(kcp).cancel(cap.capture());
        assertThat(cap.getValue().getTradeNo()).isEqualTo("KCP-TNO-1");
        assertThat(cap.getValue().getAmount()).isEqualTo(3000L);
        assertThat(cap.getValue().getRemainingBefore()).isEqualTo(8000L);
        assertThat(cap.getValue().getPayType()).isEqualTo("100000000000");
        verifyNoInteractions(tossPaymentService, orderPayLogTossMapper);
    }

    @Test
    void KCP_도_DB_상태를_먼저_바꾼_뒤_취소를_호출한다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("100000000000", 3000));
        when(gatewayRegistry.forExistingPayment("KCP")).thenReturn(kcp);
        when(kcp.cancel(any())).thenReturn(new CancelResult());
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자");

        InOrder order = inOrder(orderProdMapper, kcp);
        order.verify(orderProdMapper).updateOrderState(any());
        order.verify(kcp).cancel(any());
    }

    @Test
    void KCP_취소가_실패하면_예외로_전파돼_상태가_롤백된다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("100000000000", 3000));
        when(gatewayRegistry.forExistingPayment("KCP")).thenReturn(kcp);
        when(kcp.label()).thenReturn("NHN KCP");
        when(kcp.cancel(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KCP 결제 취소 실패: [8000] 한도초과"));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NHN KCP 결제 취소 실패");
    }

    @Test
    void KCP_잔액_불일치_같은_게이트웨이_예외는_그대로_올라가_취소가_멈춘다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("100000000000", 3000));
        when(gatewayRegistry.forExistingPayment("KCP")).thenReturn(kcp);
        when(kcp.cancel(any())).thenThrow(new IllegalStateException("KCP 거래 잔액(500원)이 시스템 기록(3000원)과 달라 자동 취소하지 않았습니다."));
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("자동 취소하지 않았습니다");
    }

    @Test
    void 환불금액이_남은_결제금액보다_크면_DB도_PG도_건드리지_않는다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("100000000000", 3000));
        when(gatewayRegistry.forExistingPayment("KCP")).thenReturn(kcp);
        when(orderProdMapper.sumRefundedAmount(ORDER_NO)).thenReturn(1000L);   // 남은 2,000 < 환불 3,000

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("남은 결제금액");
        verify(orderProdMapper, never()).updateOrderState(any());
        verify(kcp, never()).cancel(any());
    }

    @Test
    void 구현체를_모르는_PG_는_자동_취소하지_않는다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        OrderPay pay = kcpPay("100000000000", 3000);
        pay.setPgCode("NOPE");
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(pay);
        when(gatewayRegistry.forExistingPayment("NOPE")).thenReturn(null);

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOPE");
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void KCP_가상계좌는_자동취소하지_않고_안내_문구에_결제취소_호출_생략을_쓴다() throws Exception {
        givenLine(line("주문취소", 3000, 3000, 1));
        when(orderPayMapper.selectPgByOrderNo(ORDER_NO)).thenReturn(kcpPay("001000000000", 3000));

        assertThatThrownBy(() -> service.confirmOne(ORDER_NO, PROD_NO, false, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("가상계좌").hasMessageContaining("결제취소 호출 생략");
        verify(orderProdMapper, never()).updateOrderState(any());
    }
}
