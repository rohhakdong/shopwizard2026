package com.shopwizard.payment.service;

import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PayMethodOption;
import com.shopwizard.payment.model.PaymentResult;

import java.util.List;
import java.util.Map;

/**
 * 결제 대행사(PG) 하나. 주문 저장·환불 로직은 이 인터페이스만 알고, PG 별 규격(토스 REST, KCP 서명 API …)은 구현체에 숨긴다.
 * 새 PG 는 구현체를 빈으로 등록하고 {@code payment.enabled-pgs} 에 코드를 추가하면 켜진다.
 */
public interface PaymentGateway {

    /** tOrdOrderPay.PgCode 에 저장되는 코드(TOSS, KCP …) */
    String code();

    /** 관리자 화면 표시용 이름 */
    String label();

    /** 이 PG 로 받을 수 있는 결제수단(주문서에 표시) */
    List<PayMethodOption> methods();

    /** 화면(결제창 호출)에 내려주는 공개 설정 — 비밀값(시크릿 키·인증서·개인키)은 절대 넣지 않는다. */
    Map<String, Object> clientConfig();

    /** 결제 승인. 실패하면 예외(승인되지 않았음). 성공하면 PG 가 응답한 금액·상태를 담은 결과. */
    PaymentResult approve(ApproveRequest request) throws Exception;

    /** 결제 취소(전체/부분). 실패하면 예외. */
    CancelResult cancel(CancelRequest request) throws Exception;
}
