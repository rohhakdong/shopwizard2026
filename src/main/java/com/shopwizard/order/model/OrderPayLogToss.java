package com.shopwizard.order.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * tOrdOrderPayLogToss - 토스페이먼츠 결제 승인 응답 로그.
 * 기존 tOrdOrderPayLog(KCP 전용 콜백 로그)와는 컬럼 체계가 달라 별도 테이블로 분리.
 */
@Data
public class OrderPayLogToss {
    private Integer logId;
    private Integer orderNo;
    private String paymentKey;
    private String orderId;
    private String method;
    private String status;
    private Long totalAmount;
    private Long balanceAmount;
    private Long suppliedAmount;
    private Long vat;
    private String currency;
    private String requestedAt;
    private String approvedAt;
    private String cardCompany;
    private String cardNumber;
    private String cardApproveNo;
    private Integer cardInstallmentPlanMonths;
    private String receiptUrl;
    private Boolean success;
    private String resultCode;
    private String resultMsg;
    private String rawResponse;
    private String remark;
    private String registId;
    private String registName;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 토스 결제 승인/조회 응답(tossResp)으로부터 로그 레코드를 구성한다. */
    public static OrderPayLogToss from(Integer orderNo, Map<String, Object> tossResp,
                                        String registId, String registName) {
        Map<String, Object> card          = asMap(tossResp.get("card"));
        Map<String, Object> receipt       = asMap(tossResp.get("receipt"));

        OrderPayLogToss log = new OrderPayLogToss();
        log.setOrderNo(orderNo);
        log.setPaymentKey(str(tossResp.get("paymentKey")));
        log.setOrderId(str(tossResp.get("orderId")));
        log.setMethod(str(tossResp.get("method")));
        log.setStatus(str(tossResp.get("status")));
        log.setTotalAmount(numToLong(tossResp.get("totalAmount")));
        log.setBalanceAmount(numToLong(tossResp.get("balanceAmount")));
        log.setSuppliedAmount(numToLong(tossResp.get("suppliedAmount")));
        log.setVat(numToLong(tossResp.get("vat")));
        log.setCurrency(str(tossResp.get("currency")));
        log.setRequestedAt(str(tossResp.get("requestedAt")));
        log.setApprovedAt(str(tossResp.get("approvedAt")));
        log.setCardCompany(card != null ? str(card.get("company")) : null);
        log.setCardNumber(card != null ? str(card.get("number")) : null);
        log.setCardApproveNo(card != null ? str(card.get("approveNo")) : null);
        log.setCardInstallmentPlanMonths(card != null ? intVal(card.get("installmentPlanMonths")) : null);
        log.setReceiptUrl(receipt != null ? str(receipt.get("url")) : null);
        log.setSuccess(true);
        log.setRawResponse(toJson(tossResp));
        log.setRegistId(registId);
        log.setRegistName(registName);
        return log;
    }

    /** 토스 응답의 ISO-8601 시각(예: 2026-08-05T16:50:55+09:00)을 MySQL DATETIME 형식으로 변환 */
    public static String toMysqlDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long numToLong(Object o) {
        return (o instanceof Number) ? ((Number) o).longValue() : null;
    }

    private static Integer intVal(Object o) {
        return (o instanceof Number) ? ((Number) o).intValue() : null;
    }

    private static String toJson(Map<String, Object> resp) {
        try {
            return OBJECT_MAPPER.writeValueAsString(resp);
        } catch (Exception e) {
            return null;
        }
    }
}
