package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 주문라인 1건을 환불완료로 전환한다 (주문취소 또는 반품완료 → 환불완료).
 * 원본 상태 확인은 {@link OrderProdMapper#selectLineRaw}로 직접 한다(대상 매퍼에 origin-state
 * WHERE 가드가 없음). 그 주문의 모든 라인이 환불완료에 도달하면 헤더도 승격한다
 * ({@link OrderProdMapper#countOrderStateDiffer}).
 *
 * <p><b>PG 환불</b>: 스토어프론트에서 토스로 결제한 주문(tOrdOrderPay.PgCode='TOSS')이면 라인 결제금액만큼
 * 토스 <b>부분취소</b>를 호출한다. 샵링커 등 마켓 주문은 PG 가 우리 쪽에 없어(환불은 마켓 정산 몫) 상태만
 * 바꾼다. {@code skipPg} 이면 토스 호출을 생략한다(가상계좌 등 자동 취소 불가, 이미 별도 환불).</p>
 *
 * <p>순서: 검증(결제 조회, 금액, 가상계좌 여부) → <b>DB 상태 변경(행 잠금)</b> → 토스 취소 호출. 토스 취소가
 * 예외를 던지면 REQUIRES_NEW 트랜잭션이 롤백돼 상태가 그대로 남는다. DB 를 먼저 바꿔 행 잠금을 잡아두는 이유:
 * 토스가 취소 직후 보내는 CANCELED 웹훅이 이 라인을 덮어쓰지 못하고 커밋을 기다렸다가 최신 상태를 보게 하려는
 * 것이다(TossWebhookService 참고). 토스 성공 후 커밋이 실패하는 드문 경우도 같은 Idempotency-Key 로 재시도하면
 * 토스가 첫 응답을 그대로 돌려줘 이중 환불 없이 복구된다.</p>
 *
 * <p>OrderService(오케스트레이션, confirmRefundBatch)와 다른 빈으로 분리한 이유는 건별
 * {@code REQUIRES_NEW} 격리를 위해서다.</p>
 */
@Service
@RequiredArgsConstructor
public class RefundConfirmService {

    private static final Set<String> REFUNDABLE_STATES = Set.of("주문취소", "반품완료");
    /** 가상계좌: 환불받을 계좌 정보(refundReceiveAccount)가 필요해 우리 쪽에서 자동 취소할 수 없다. */
    private static final String VBANK_PAY_TYPE = "001000000000";
    private static final Set<String> CANCELABLE_PG_STATUS = Set.of("DONE", "PARTIAL_CANCELED");

    private final OrderMapper orderMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderPayMapper orderPayMapper;
    private final OrderPayLogTossMapper orderPayLogTossMapper;
    private final TossPaymentService tossPaymentService;
    private final OrderLineLock lineLock;   // 상태 확인~갱신 사이에 다른 요청이 끼어들지 못하게 행 잠금

    /** 성공하면 null, 업무상 실패는 사유 문자열 반환. 토스 호출 실패 등은 예외 → 이 건만 롤백된다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String confirmOne(Integer orderNo, Integer orderProdNo, boolean skipPg,
                             String registId, String registName) throws Exception {
        // 헤더 → 라인 순으로 잠근다. 같은 라인의 중복 환불확정은 여기서 대기했다가 상태가 이미 바뀐 것을 보고 끝난다.
        OrderProd op = lineLock.lockHeaderAndLine(orderNo, orderProdNo);
        if (op == null) {
            return "주문라인을 찾을 수 없습니다.";
        }
        if (!REFUNDABLE_STATES.contains(op.getOrderState())) {
            return "환불확정 가능한 상태가 아닙니다. (현재 상태: " + op.getOrderState() + ")";
        }

        // 1) 검증 — DB 를 건드리기 전에 끝내서, 실패해도 잠금/롤백 부담이 없게 한다.
        PgRefund pg = skipPg ? null : preparePgRefund(orderNo, op);

        // 2) DB 상태 변경 (행 잠금 확보)
        Map<String, Object> upd = new HashMap<>();
        upd.put("orderNo", orderNo);
        upd.put("orderProdNo", orderProdNo);
        upd.put("orderState", "환불완료");
        upd.put("changeId", registId);
        upd.put("changeName", registName);
        orderProdMapper.updateOrderState(upd);

        Map<String, Object> diff = new HashMap<>();
        diff.put("orderNo", orderNo);
        diff.put("orderState", "환불완료");
        if (orderProdMapper.countOrderStateDiffer(diff) == 0) {
            Map<String, Object> headerUpd = new HashMap<>();
            headerUpd.put("orderNo", orderNo);
            headerUpd.put("orderState", "환불완료");
            headerUpd.put("changeId", registId);
            headerUpd.put("changeName", registName);
            orderMapper.updateOrderState(headerUpd);
        }

        // 3) 토스 부분취소 — 예외가 나면 위 DB 변경이 롤백된다.
        if (pg != null && pg.callApi) {
            Map<String, Object> resp;
            try {
                resp = tossPaymentService.cancelPartial(pg.paymentKey, "환불확정", pg.amount,
                        "refund-" + orderNo + "-" + orderProdNo);
            } catch (ResponseStatusException e) {
                throw new IllegalStateException("토스 결제 취소 실패: " + e.getReason(), e);
            }
            orderPayLogTossMapper.insert(OrderPayLogToss.from(orderNo, resp, registId, registName));
        }
        return null;
    }

    /** 토스 결제 주문이면 환불 금액/상태를 확인해 호출 계획을 만든다. 토스 결제가 아니면 null(상태만 변경). */
    private PgRefund preparePgRefund(Integer orderNo, OrderProd op) throws Exception {
        OrderPay pay = orderPayMapper.selectTossByOrderNo(orderNo);
        if (pay == null || pay.getPgTradeNo() == null || pay.getPgTradeNo().isBlank()) {
            return null;
        }
        if (VBANK_PAY_TYPE.equals(pay.getPayType())) {
            throw new IllegalStateException("가상계좌 결제는 환불계좌 정보가 필요해 자동 취소할 수 없습니다. "
                    + "토스 대시보드에서 환불한 뒤 '토스 호출 생략'을 체크하고 처리하세요.");
        }

        int amount = nvl(op.getReciptAmt()) > 0 ? op.getReciptAmt() : nvl(op.getSalePrice()) * nvl(op.getProdQty());
        if (amount <= 0) {
            throw new IllegalStateException("환불 금액을 계산할 수 없습니다. (결제금액 0원)");
        }

        Map<String, Object> payment = tossPaymentService.getPayment(pay.getPgTradeNo());
        String status = String.valueOf(payment.get("status"));
        if ("CANCELED".equals(status)) {
            // 이미 전액 취소된 결제(토스 대시보드 취소 등) — 더 환불할 것이 없으므로 호출 없이 상태만 반영한다.
            return new PgRefund(pay.getPgTradeNo(), amount, false);
        }
        if (!CANCELABLE_PG_STATUS.contains(status)) {
            throw new IllegalStateException("토스 결제 상태가 환불 가능한 상태가 아닙니다. (상태: " + status + ")");
        }
        return new PgRefund(pay.getPgTradeNo(), amount, true);
    }

    private record PgRefund(String paymentKey, int amount, boolean callApi) {}

    private static int nvl(Integer i) { return i == null ? 0 : i; }
}
