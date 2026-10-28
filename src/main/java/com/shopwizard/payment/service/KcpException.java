package com.shopwizard.payment.service;

/**
 * KCP 호출 자체가 실패했다(연결·타임아웃·응답 파싱). {@link #isUncertain()} 이 true 면 요청이 KCP 에 도달했을 수 있어
 * 처리 여부를 알 수 없다 — 승인이면 거래 조회로 확인해야 하고, 취소면 다시 조회해서 잔액으로 판단해야 한다.
 */
public class KcpException extends Exception {

    private final boolean uncertain;

    public KcpException(String message, boolean uncertain, Throwable cause) {
        super(message, cause);
        this.uncertain = uncertain;
    }

    public boolean isUncertain() {
        return uncertain;
    }
}
