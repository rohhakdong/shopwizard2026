package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.service.KcpGateway;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 입금 기한이 지난 KCP 가상계좌 주문을 정리한다 — 계좌를 사용중지하고 주문을 취소하며 차감했던 재고를 되돌린다.
 *
 * <p>KCP 는 만료 통보를 보내지 않으므로 우리가 주기적으로 확인한다. 순서가 중요하다: <b>사용중지가 성공한 뒤에만</b> 주문을 취소한다
 * (계좌가 살아 있는데 주문만 취소하면 뒤늦은 입금이 주문 없는 돈이 된다). 사용중지가 실패하면 주문은 그대로 두고 다음 주기에 다시 시도한다.
 * 정리 직전에 입금 여부를 조회해, 마감 직전에 입금이 들어온 경우에는 취소하지 않고 지불완료로 구제한다.</p>
 *
 * <p>건별로 독립 트랜잭션(REQUIRES_NEW)이라 한 건이 실패해도 나머지는 처리된다.</p>
 */
@Slf4j
@Service
public class KcpVirtualAccountExpiryService {

    private static final String SYSTEM_ID = "SYSTEM";
    private static final String SYSTEM_NAME = "KCP가상계좌만료";
    private static final String VBANK_PAY_TYPE = "001000000000";

    private final OrderPayMapper orderPayMapper;
    private final OrderService orderService;
    private final OrderLineLock lineLock;
    private final OrderPaymentSyncService paymentSync;
    private final KcpGateway kcpGateway;
    private final KcpProperties props;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final TransactionTemplate requiresNew;

    public KcpVirtualAccountExpiryService(OrderPayMapper orderPayMapper, OrderService orderService, OrderLineLock lineLock,
                                          OrderPaymentSyncService paymentSync, KcpGateway kcpGateway, KcpProperties props,
                                          PaymentGatewayRegistry gatewayRegistry, PlatformTransactionManager txManager) {
        this.orderPayMapper = orderPayMapper;
        this.orderService = orderService;
        this.lineLock = lineLock;
        this.paymentSync = paymentSync;
        this.kcpGateway = kcpGateway;
        this.props = props;
        this.gatewayRegistry = gatewayRegistry;
        this.requiresNew = new TransactionTemplate(txManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 스케줄러가 부른다. KCP 가 꺼져 있거나 잡이 비활성이면 아무것도 하지 않는다. @return 취소하거나 지불완료로 구제한 건수 */
    public int run() {
        if (!props.isVaExpireJobEnabled() || gatewayRegistry.enabled().stream().noneMatch(g -> KcpGateway.CODE.equals(g.code()))) {
            return 0;
        }
        List<OrderPay> targets = orderPayMapper.selectExpiredKcpVbank(props.getVaExpireGraceMinutes());
        int handled = 0;
        for (OrderPay pay : targets) {
            try {
                Boolean changed = requiresNew.execute(status -> expireOne(pay));
                if (Boolean.TRUE.equals(changed)) handled++;
            } catch (Exception e) {
                // 사용중지 실패 등 — 주문은 그대로 두고 다음 주기에 다시 시도한다.
                log.warn("KCP 가상계좌 만료 처리 실패 orderNo={} tno={}: {}", pay.getOrderNo(), pay.getPgTradeNo(), e.getMessage());
            }
        }
        if (!targets.isEmpty()) log.info("KCP 가상계좌 만료 점검: 대상 {}건, 처리 {}건", targets.size(), handled);
        return handled;
    }

    /** @return 주문을 취소하거나 지불완료로 바꿨으면 true, 건너뛰었으면 false */
    boolean expireOne(OrderPay pay) {
        int orderNo = pay.getOrderNo();
        long expected = pay.getPayAmt() == null ? 0 : pay.getPayAmt().longValue();

        lineLock.lockHeader(orderNo);
        Order order = orderService.select(Map.of("orderNo", orderNo));
        if (order == null || !"주문접수".equals(order.getOrderState())) return false;   // 그 사이 입금/처리된 건

        KcpGateway.DepositStatus deposit = kcpGateway.queryDeposit(pay.getPgTradeNo());
        if (deposit.paidAmount() > 0) {
            if (deposit.paidAmount() == expected) {
                // 마감 직전 입금 — 웹훅이 아직 처리되지 않았다면 여기서 구제한다.
                paymentSync.markPaid(orderNo, order, SYSTEM_ID, SYSTEM_NAME);
                Map<String, Object> dep = new HashMap<>();
                dep.put("orderNo", orderNo);
                dep.put("payNo", pay.getPayNo());
                dep.put("amount", deposit.paidAmount());
                dep.put("changeId", SYSTEM_ID);
                dep.put("changeName", SYSTEM_NAME);
                orderPayMapper.markDeposited(dep);
                log.info("KCP 가상계좌 만료 점검 중 입금을 확인해 지불완료로 바꿨습니다. orderNo={} tno={}", orderNo, pay.getPgTradeNo());
                return true;
            }
            log.error("KCP 가상계좌 입금액이 주문 금액과 달라 만료 처리하지 않았습니다 — 수동 확인 필요. orderNo={} 주문금액={} 입금액={}",
                    orderNo, expected, deposit.paidAmount());
            return false;
        }

        // 계좌를 먼저 닫고(실패하면 예외 → 주문은 그대로), 닫힌 뒤에만 주문을 취소한다.
        CancelRequest cancel = new CancelRequest();
        cancel.setTradeNo(pay.getPgTradeNo());
        cancel.setPayType(VBANK_PAY_TYPE);
        cancel.setAmount(expected);
        cancel.setReason("미입금 기한 만료");
        try {
            kcpGateway.cancel(cancel);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        paymentSync.cancelBeforeShipment(orderNo, SYSTEM_ID, SYSTEM_NAME);
        log.info("KCP 가상계좌 미입금 만료 → 주문취소·재고 복원. orderNo={} tno={}", orderNo, pay.getPgTradeNo());
        return true;
    }
}
