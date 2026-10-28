package com.shopwizard.payment.service;

import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PayMethodOption;
import com.shopwizard.payment.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * NHN KCP. PC 표준결제창(카드·계좌이체·제휴 간편결제)의 승인·취소를 맡는다.
 *
 * <ul>
 *   <li><b>승인</b>: 결제창 인증 결과(enc_data/enc_info/tran_cd)에 서버가 계산한 금액(ordr_mony)·결제수단(pay_type)·주문번호(ordr_no)를
 *       붙여 보낸다. KCP 가 창에서 인증된 값과 대조하므로 클라이언트가 금액을 바꿔도 승인되지 않는다. 응답이 없으면(타임아웃)
 *       거래 조회로 승인 여부를 확인해, 승인됐으면 그 결과로 이어가고 아니면 실패로 처리한다.</li>
 *   <li><b>취소</b>: KCP 는 멱등키가 없으므로 취소 전에 거래를 조회해 잔액(rem_mny)을 읽고, 호출자가 알려준 "직전 잔액"과 비교해
 *       이중 취소를 막는다({@link CancelRequest#getRemainingBefore()}).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KcpGateway implements PaymentGateway {

    public static final String CODE = "KCP";

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter APP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter MYSQL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern SITE_CD = Pattern.compile("[A-Z0-9]{5}");

    /** 주문서 결제수단 → KCP pay_type */
    private static final Map<String, String> PAY_TYPE = Map.of("card", "PACA", "trans", "PABK");
    /** PC 결제창 pay_method(12자리 비트코드) */
    private static final Map<String, String> PC_PAY_METHOD = Map.of("card", "100000000000", "trans", "010000000000");

    private final KcpProperties props;
    private final KcpCredentials credentials;
    private final KcpClient client;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "NHN KCP";
    }

    @Override
    public List<PayMethodOption> methods() {
        // 간편결제(토스페이·카카오페이 등)는 카드 결제창 안에 계약된 제휴사가 함께 보인다.
        return List.of(new PayMethodOption("card", "신용카드·간편결제"), new PayMethodOption("trans", "계좌이체"));
    }

    @Override
    public Map<String, Object> clientConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", props.prod() ? "prod" : "test");
        m.put("siteCd", props.getSiteCd());
        m.put("siteName", props.getSiteName() == null || props.getSiteName().isBlank() ? "SHOP" : props.getSiteName());
        m.put("scriptUrl", props.scriptUrlResolved());
        m.put("currency", "410");
        m.put("payMethodCodes", PC_PAY_METHOD);
        return m;
    }

    @Override
    public void verifyReady() {
        String mode = props.getMode() == null ? "" : props.getMode().trim().toLowerCase();
        if (!mode.equals("test") && !mode.equals("prod")) {
            throw new IllegalStateException("kcp.mode 는 test 또는 prod 여야 합니다.");
        }
        String site = props.getSiteCd() == null ? "" : props.getSiteCd().trim();
        if (!SITE_CD.matcher(site).matches()) {
            throw new IllegalStateException("kcp.site-cd(KCP_SITE_CD)는 영문 대문자·숫자 5자리여야 합니다.");
        }
        if (props.prod() && KcpProperties.TEST_SITE_CD.equals(site)) {
            throw new IllegalStateException("운영 모드(kcp.mode=prod)에서는 테스트 사이트코드(T0000)를 쓸 수 없습니다.");
        }
        credentials.verifyReady();
    }

    @Override
    public PaymentResult approve(ApproveRequest req) throws Exception {
        String payType = PAY_TYPE.get(req.getPayMethod());
        if (payType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 결제수단입니다.");
        }
        String resCd = req.payload("resCd");
        String encData = req.payload("encData");
        String encInfo = req.payload("encInfo");
        String tranCd = req.payload("tranCd");
        if (!"0000".equals(resCd) || isBlank(encData) || isBlank(encInfo) || isBlank(tranCd)
                || isBlank(req.getOrderCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 정보가 올바르지 않습니다.");
        }

        Map<String, Object> resp;
        try {
            resp = client.approve(encData, encInfo, tranCd, req.getAmount(), payType, req.getOrderCode());
        } catch (KcpException e) {
            if (!e.isUncertain()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "결제 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            }
            resp = recoverApproval(req, payType);
        }

        if (!"0000".equals(str(resp.get("res_cd")))) {
            log.warn("KCP 승인 거절 order={} res_cd={}", req.getOrderCode(), str(resp.get("res_cd")));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다: " + str(resp.get("res_msg")));
        }
        Long approved = toLong(resp.get("amount"));
        if (approved == null || approved != req.getAmount()) {
            // KCP 가 서버 금액으로 대조하므로 일어나선 안 되는 일 — 일어났다면 승인된 돈을 되돌리고 거절한다.
            cancelQuietly(str(resp.get("tno")), payType, "승인 금액 불일치로 자동 취소");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }
        return toResult(resp, req.getOrderCode(), payType);
    }

    /** 승인 응답을 못 받았다 — 거래 조회로 확인한다. 승인돼 있으면 그 조회 결과로 이어가고, 아니면 실패. */
    private Map<String, Object> recoverApproval(ApproveRequest req, String payType) {
        LocalDate today = LocalDate.now(KST);
        for (LocalDate day : new LocalDate[] {today, today.minusDays(1)}) {   // 자정 전후 승인도 찾는다
            try {
                Map<String, Object> r = client.inquiryByOrder(day.format(YMD), req.getOrderCode(), payType);
                Long amount = toLong(r.get("amount"));
                if ("0000".equals(str(r.get("res_cd"))) && amount != null && amount == req.getAmount() && !isCanceled(r)) {
                    log.warn("KCP 승인 응답이 없었지만 조회로 승인을 확인했습니다. order={} tno={}", req.getOrderCode(), r.get("tno"));
                    return r;
                }
            } catch (KcpException ignore) {
                break;
            }
        }
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                "결제 결과를 확인하지 못했습니다. 결제가 되었는지 확인 후 다시 시도해 주세요. (주문번호: " + req.getOrderCode() + ")");
    }

    @Override
    public CancelResult cancel(CancelRequest req) throws Exception {
        String payType = payTypeOfStored(req.getPayType());
        Long amount = req.getAmount();

        Map<String, Object> inq = client.inquiryByTno(req.getTradeNo(), payType);
        if (!"0000".equals(str(inq.get("res_cd")))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "KCP 거래 조회에 실패했습니다: [" + str(inq.get("res_cd")) + "] " + str(inq.get("res_msg")));
        }
        long rem = remaining(inq);

        if (rem == 0 && amount == null) {
            return done(req, inq, null, true);                      // 이미 전액 취소됨
        }
        Long before = req.getRemainingBefore();
        if (before != null) {
            if (amount != null && rem == before - amount) {
                return done(req, inq, amount, true);                // 직전에 이 취소가 성공했던 것 — 다시 호출하지 않는다
            }
            if (rem != before) {
                throw new IllegalStateException("KCP 거래 잔액(" + rem + "원)이 시스템 기록(" + before
                        + "원)과 달라 자동 취소하지 않았습니다. KCP 에서 취소 내역을 확인한 뒤 '결제취소 호출 생략'으로 처리하세요.");
            }
        }
        if (rem <= 0) {
            throw new IllegalStateException("KCP 거래에 취소할 수 있는 잔액이 없습니다.");
        }
        if (amount != null && amount > rem) {
            throw new IllegalStateException("취소 금액(" + amount + "원)이 KCP 거래 잔액(" + rem + "원)보다 큽니다.");
        }

        boolean full = amount == null || amount == rem;
        Map<String, Object> resp;
        try {
            resp = client.cancel(req.getTradeNo(), full ? "STSC" : "STPC", full ? null : amount, full ? null : rem, req.getReason());
        } catch (KcpException e) {
            // 취소가 됐는지 알 수 없다 — 호출자는 이 예외를 실패로 다루고(상태 롤백), 재시도 때 위의 잔액 비교가 이중 취소를 막는다.
            throw new IllegalStateException("KCP 취소 결과를 확인하지 못했습니다. 잠시 후 다시 시도하면 잔액을 확인해 이어서 처리합니다.", e);
        }
        if (!"0000".equals(str(resp.get("res_cd")))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "KCP 결제 취소 실패: [" + str(resp.get("res_cd")) + "] " + str(resp.get("res_msg")));
        }
        return done(req, resp, full ? Long.valueOf(rem) : amount, false);
    }

    private CancelResult done(CancelRequest req, Map<String, Object> raw, Long canceled, boolean already) {
        Map<String, Object> copy = new HashMap<>(raw);
        copy.put("already_canceled", already);
        CancelResult r = new CancelResult();
        r.setPgCode(CODE);
        r.setTradeNo(req.getTradeNo());
        r.setCanceledAmount(canceled);
        r.setRaw(copy);
        return r;
    }

    /** 주문 저장 실패 같은 보상 취소 — 실패해도 원래 오류를 가리지 않도록 삼키고 로그만 남긴다(수동 대사 대상). */
    private void cancelQuietly(String tno, String payType, String reason) {
        try {
            client.cancel(tno, "STSC", null, null, reason);
        } catch (Exception e) {
            log.error("KCP 자동 취소 실패 tno={} — 수동 확인 필요", tno);
        }
    }

    /** KCP 응답 → PG 공통 결과. 승인 응답과 거래 조회 응답 모두 같은 필드명을 쓴다. */
    static PaymentResult toResult(Map<String, Object> resp, String orderCode, String payType) {
        PaymentResult r = new PaymentResult();
        r.setPgCode(CODE);
        r.setTradeNo(str(resp.get("tno")));
        r.setOrderCode(orderCode);
        r.setAmount(toLong(resp.get("amount")));
        r.setStatus(PaymentResult.STATUS_DONE);
        boolean bank = "PABK".equals(payType);
        r.setMethod(bank ? "TRANS" : (isBlank(str(resp.get("card_other_pay_type"))) ? "CARD" : "EASY"));
        r.setCardComp(bank ? null : str(resp.get("card_name")));
        r.setApproveNo(bank ? null : str(resp.get("app_no")));
        r.setApprovedAt(appTime(str(resp.get("app_time"))));
        r.setReciptBank(bank ? str(resp.get("bank_name")) : null);
        r.setRaw(resp);
        return r;
    }

    static String payTypeOfStored(String storedPayType) {
        if (storedPayType == null) return "PACA";
        return switch (storedPayType) {
            case "010000000000" -> "PABK";
            case "001000000000" -> "PAVC";
            default -> "PACA";
        };
    }

    /** 취소 가능 잔액. 전액 취소된 거래는 0, 잔액 필드가 없으면 승인 금액. */
    static long remaining(Map<String, Object> inq) {
        if (isCanceled(inq)) return 0;
        Long rem = toLong(inq.get("rem_mny"));
        if (rem != null) return rem;
        Long amount = toLong(inq.get("amount"));
        return amount == null ? 0 : amount;
    }

    static boolean isCanceled(Map<String, Object> r) {
        return "STSC".equals(str(r.get("stat_ca_cd"))) || "STSC".equals(str(r.get("stat_bk_cd")));
    }

    private static String appTime(String yyyymmddhhmmss) {
        if (isBlank(yyyymmddhhmmss)) return null;
        try {
            return java.time.LocalDateTime.parse(yyyymmddhhmmss, APP_TIME).format(MYSQL);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null || String.valueOf(o).isBlank()) return null;
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
