package com.shopwizard.payment.model;

import lombok.Data;

import java.util.Map;

/**
 * PG 승인 결과를 PG 에 상관없이 같은 모양으로 담는다. 주문 저장({@code CheckoutService})은 이 값만 본다.
 * 금액·상태는 클라이언트가 보낸 값이 아니라 <b>PG 가 응답한 값</b>이다.
 */
@Data
public class PaymentResult {

    /** 결제 대기(가상계좌 발급 후 입금 전) — 이 상태면 주문은 "주문접수", 입금 통보로 "지불완료"가 된다. */
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_WAITING_DEPOSIT = "WAITING_FOR_DEPOSIT";

    /** TOSS / KCP … (tOrdOrderPay.PgCode) */
    private String pgCode;
    /** PG 거래 식별자 — 토스 paymentKey / KCP tno (tOrdOrderPay.PgTradeNo) */
    private String tradeNo;
    /** 가맹점 주문번호 — 토스 orderId / KCP ordr_idxx (tOrdOrderPay.PgOrderCode) */
    private String orderCode;
    /** PG 가 승인한 금액 */
    private Long amount;
    private String status;
    /** CARD | TRANS | VBANK | EASY | … */
    private String method;
    private String cardComp;
    private String approveNo;
    /** yyyy-MM-dd HH:mm:ss (MySQL DATETIME 형식) */
    private String approvedAt;
    /** 계좌이체 은행 / 가상계좌 은행 */
    private String reciptBank;
    /** PG 응답 원본 — PG 별 결제 로그 테이블에 저장한다 */
    private Map<String, Object> raw;

    public boolean paid() {
        return STATUS_DONE.equals(status);
    }
}
