package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.payment.service.KcpGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * KCP 가상계좌 입금 통보(웹훅) 처리. 웹훅 본문은 <b>트리거로만</b> 쓰고 입금 사실은 KCP 거래조회(ipkum_money)로 다시 확인한 값으로 반영한다
 * (토스 웹훅과 같은 원칙). 모든 경로가 멱등이다 — 같은 통보가 재전송돼도 상태·기록이 두 번 바뀌지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KcpWebhookService {

    private static final String SYSTEM_ID = "SYSTEM";
    private static final String SYSTEM_NAME = "KCP웹훅";
    /** 취소·환불 단계 — 이런 주문에 입금이 오면 자동으로 되살리지 않고 수동 환불 대상으로 알린다. */
    private static final Set<String> CANCELED_STATES = Set.of("주문취소", "환불완료");

    /** 처리 결과 — 컨트롤러가 KCP 에 보낼 응답을 정한다. */
    public enum Outcome {
        /** 입금을 확인해 지불완료로 바꿨다 */
        PAID,
        /** 이미 처리된 통보(재전송) */
        ALREADY_PROCESSED,
        /** 자동 처리할 수 없어 사람이 확인해야 하지만 KCP 가 다시 보낼 필요는 없다(오류 로그를 남겼다) */
        MANUAL,
        /** 아직 처리할 수 없다 — KCP 가 재전송하도록 실패로 응답한다(결제건이 저장되기 전 등) */
        RETRY
    }

    private final OrderPayMapper orderPayMapper;
    private final OrderService orderService;
    private final OrderLineLock lineLock;   // 헤더 → 라인 순서의 행 잠금(취소/환불확정과 같은 규칙)
    private final OrderPaymentSyncService paymentSync;
    private final KcpGateway kcpGateway;

    /**
     * @param tno       KCP 거래번호
     * @param notified  통보된 입금액(ipgm_mnyx) — 참고용. 실제 판단은 KCP 조회값으로 한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public Outcome handleDeposit(String tno, Long notified) {
        if (tno == null || tno.isBlank()) return Outcome.MANUAL;

        OrderPay pay = orderPayMapper.selectByPgTradeNoAny(KcpGateway.CODE, tno);
        if (pay == null || pay.getOrderNo() == null) {
            // 이 시스템 주문이 아니거나, 계좌 발급 직후라 주문 저장이 끝나기 전일 수 있다 — 재전송을 받아 다시 본다.
            log.warn("KCP 입금 통보: 결제건을 찾지 못했습니다. tno={} (재전송 대기)", tno);
            return Outcome.RETRY;
        }
        int orderNo = pay.getOrderNo();
        long expected = pay.getPayAmt() == null ? 0 : pay.getPayAmt().longValue();

        // 입금 사실은 KCP 에 다시 묻는다(실패하면 예외 → 재전송). 행 잠금을 잡기 전에 네트워크 호출을 끝내 잠금 시간을 줄인다.
        KcpGateway.DepositStatus deposit = kcpGateway.queryDeposit(tno);

        lineLock.lockHeader(orderNo);
        Order order = orderService.select(Map.of("orderNo", orderNo));
        if (order == null) {
            log.error("KCP 입금 통보: 주문을 찾지 못했습니다. orderNo={} tno={}", orderNo, tno);
            return Outcome.MANUAL;
        }

        String state = order.getOrderState();
        if (CANCELED_STATES.contains(state)) {
            if (deposit.paidAmount() > 0) {
                log.error("KCP 가상계좌에 입금됐지만 주문이 이미 {} 상태입니다 — 수동 환불 필요. orderNo={} tno={} 입금액={}",
                        state, orderNo, tno, deposit.paidAmount());
                return Outcome.MANUAL;
            }
            return Outcome.ALREADY_PROCESSED;
        }
        if (!"주문접수".equals(state)) {
            return Outcome.ALREADY_PROCESSED;   // 지불완료 이후 단계 — 재전송이므로 아무것도 하지 않는다
        }

        if (deposit.paidAmount() <= 0) {
            // 통보는 왔는데 KCP 는 입금 전이라고 한다 — 통보를 믿지 않는다. 사람이 확인하도록 남기고 재전송을 멈춘다.
            log.error("KCP 입금 통보와 거래조회가 어긋납니다(조회상 입금 0원). orderNo={} tno={} 통보금액={}", orderNo, tno, notified);
            return Outcome.MANUAL;
        }
        if (deposit.paidAmount() != expected) {
            log.error("KCP 입금액이 주문 금액과 다릅니다 — 지불완료로 바꾸지 않았습니다. orderNo={} tno={} 주문금액={} 입금액={}",
                    orderNo, tno, expected, deposit.paidAmount());
            return Outcome.MANUAL;
        }
        if (notified != null && notified != deposit.paidAmount()) {
            log.warn("KCP 통보 입금액({})과 조회 입금액({})이 다릅니다. 조회값을 따릅니다. orderNo={}", notified, deposit.paidAmount(), orderNo);
        }

        paymentSync.markPaid(orderNo, order, SYSTEM_ID, SYSTEM_NAME);
        Map<String, Object> dep = new HashMap<>();
        dep.put("orderNo", orderNo);
        dep.put("payNo", pay.getPayNo());
        dep.put("amount", deposit.paidAmount());
        dep.put("changeId", SYSTEM_ID);
        dep.put("changeName", SYSTEM_NAME);
        orderPayMapper.markDeposited(dep);
        log.info("KCP 가상계좌 입금 확인 → 지불완료. orderNo={} tno={} 금액={}", orderNo, tno, deposit.paidAmount());
        return Outcome.PAID;
    }
}
