package com.shopwizard.ship.service;

import com.shopwizard.external.shoplinker.ShoplinkerHttp;
import com.shopwizard.external.shoplinker.ShoplinkerInvoiceRequest;
import com.shopwizard.external.shoplinker.ShoplinkerInvoiceResponse;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.product.mapper.SlinkLogMapper;
import com.shopwizard.product.model.SlinkLog;
import com.shopwizard.ship.mapper.SlnkInvoiceMapper;
import com.shopwizard.ship.model.SlnkInvoiceLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 송장입력 → 샵링커 송장 전송. 레거시 {@code ShipDirectServiceImpl#runInsertSlnkInvoice} 이식: 샵링커 주문코드(SlnkOrderCode)가 있는 줄에
 * 택배사 <b>이름</b>과 송장번호를 GET 으로 알린다.
 *
 * <p>운영 샵링커에 반영되는 외부 연동이라 기본 모드는 {@code dry-run}(요청만 만들어 이력에 남기고 보내지 않는다)이고 {@code live} 는 계정 ID 가
 * 있어야만 동작한다. 호출은 <b>송장 저장이 커밋된 뒤</b>에 하고(행 잠금을 HTTP 호출 동안 쥐지 않는다), 결과는 이력({@code tPrdSlinkLog},
 * LogType '송장')에 남기며 전송이 실패해도 송장 저장은 되돌리지 않는다 — 실패·미전송 줄은 {@link #pending} 으로 찾아 {@link #resend} 한다.</p>
 */
@Service
public class SlnkInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(SlnkInvoiceService.class);

    public static final String SENT = "sent";
    public static final String FAILED = "failed";
    public static final String DRY = "dry";
    public static final String SKIP = "skip";
    /** 미전송 조회 기간(일). */
    public static final int PENDING_DAYS = 14;
    public static final int PENDING_LIMIT = 200;
    public static final int MAX_RESEND = 100;
    private static final int RESULT_MESSAGE_MAX = 255;   // tPrdSlinkLog.ResultMessag varchar(255)

    /** 한 줄의 전송 결과. status: sent 전송 성공 · failed 실패 · dry 시험 모드(보내지 않음) · skip 대상 아님. */
    public record Result(String status, String message) {}

    private final ShoplinkerHttp http;
    private final SlinkLogMapper logMapper;
    private final SlnkInvoiceMapper mapper;

    private final String invoiceUrl;
    private final String mode;
    private final String customerId;
    private final int timeoutSec;
    private final Set<String> excludedChnl;

    public SlnkInvoiceService(ShoplinkerHttp http, SlinkLogMapper logMapper, SlnkInvoiceMapper mapper,
                              @Value("${shoplinker.invoice-url}") String invoiceUrl,
                              @Value("${shoplinker.invoice-mode:dry-run}") String mode,
                              @Value("${shoplinker.customer-id:}") String customerId,
                              @Value("${shoplinker.invoice-timeout-sec:10}") int timeoutSec,
                              @Value("${shoplinker.invoice-exclude-chnl:}") String excludedChnl) {
        this.http = http;
        this.logMapper = logMapper;
        this.mapper = mapper;
        this.invoiceUrl = invoiceUrl;
        this.mode = "live".equalsIgnoreCase(mode == null ? "" : mode.trim()) ? "live" : "dry-run";   // 모르는 값은 안전한 쪽
        this.customerId = customerId == null ? "" : customerId.trim();
        this.timeoutSec = timeoutSec;
        this.excludedChnl = excludedChnl == null ? Set.of()
                : Arrays.stream(excludedChnl.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public String mode() { return mode; }

    private boolean isLive() { return "live".equals(mode); }

    /** 줄 한 건을 읽어 전송한다(송장 저장 커밋 뒤에 부른다). 줄이 없으면 skip. */
    public Result pushLine(Integer orderNo, Integer orderProdNo, Integer orderChangeNo, AuthSession.Actor actor) {
        SlnkInvoiceLine line = mapper.selectLine(orderNo, orderProdNo, orderChangeNo);
        return line == null ? new Result(SKIP, "줄을 찾을 수 없습니다.") : push(line, actor);
    }

    /** 전송 대상 판정 + 호출 + 이력. 어떤 경우에도 예외를 던지지 않는다(송장 저장 결과를 가리지 않게). */
    public Result push(SlnkInvoiceLine line, AuthSession.Actor actor) {
        String why = skipReason(line);
        if (why != null) return new Result(SKIP, why);

        String name = deliveryName(line);
        ShoplinkerInvoiceRequest.Built built;
        try {
            built = ShoplinkerInvoiceRequest.build(new ShoplinkerInvoiceRequest.Config(invoiceUrl, customerId), line.getSlnkOrderCode(), name, line.getInvNo());
        } catch (IllegalArgumentException e) {
            return new Result(SKIP, e.getMessage());
        }
        if (!isLive()) {
            String msg = "시험 모드: 샵링커로 보내지 않았습니다.";
            record(line, name, "dry", msg, built.logUrl(), actor);
            return new Result(DRY, msg);
        }
        if (customerId.isEmpty()) {
            String msg = "샵링커 계정 ID(SHOPLINKER_CUSTOMER_ID)가 설정되지 않아 보내지 못했습니다.";
            record(line, name, "error", msg, built.logUrl(), actor);
            return new Result(FAILED, msg);
        }
        try {
            ShoplinkerInvoiceResponse resp = ShoplinkerInvoiceResponse.parse(http.get(built.url(), timeoutSec));
            record(line, name, resp.ok() ? "true" : "false", resp.message(), built.logUrl(), actor);
            return new Result(resp.ok() ? SENT : FAILED, resp.ok() ? "샵링커에 전송했습니다." : "샵링커 응답: " + resp.message());
        } catch (Exception e) {
            log.warn("샵링커 송장 전송 오류 orderNo={} prodNo={} changeNo={}: {}", line.getOrderNo(), line.getOrderProdNo(), line.getOrderChangeNo(), e.toString());
            String msg = "전송 오류: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " - " + e.getMessage());
            record(line, name, "error", msg, built.logUrl(), actor);
            return new Result(FAILED, msg);
        }
    }

    /** 전송 대상이 아니면 그 이유, 대상이면 null. */
    String skipReason(SlnkInvoiceLine line) {
        if (line.getSlnkOrderCode() == null || line.getSlnkOrderCode().isBlank()) return "샵링커 주문이 아니어서 보내지 않았습니다.";
        if (line.getChnlCode() != null && excludedChnl.contains(line.getChnlCode())) return "송장을 샵링커로 보내지 않는 채널입니다.";
        if (isBlank(line.getInvNo()) || isBlank(deliveryName(line))) return "택배사 또는 송장번호가 없어 보내지 않았습니다.";
        return null;
    }

    /** 샵링커에 보낼 택배사 이름 — 마스터의 "샵링커 택배사명"이 있으면 그것, 없으면 우리 택배사명. */
    static String deliveryName(SlnkInvoiceLine line) {
        return !isBlank(line.getSlnkDeliName()) ? line.getSlnkDeliName().trim() : line.getDeliCompName() == null ? "" : line.getDeliCompName().trim();
    }

    // ── 미전송·실패 재전송 ────────────────────────────────────

    /** 송장은 저장됐지만 샵링커로 나갔다는 성공 이력이 없는 줄(최근 {@value #PENDING_DAYS}일). 범위 값은 null = 제한 없음. */
    public Map<String, Object> pending(String shopCode, String chnlCode, String svcCode) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> ex = List.copyOf(excludedChnl);
        out.put("mode", mode);
        out.put("days", PENDING_DAYS);
        out.put("total", mapper.countPending(PENDING_DAYS, ex, blank(shopCode), blank(chnlCode), blank(svcCode)));
        out.put("rows", mapper.selectPending(PENDING_DAYS, ex, blank(shopCode), blank(chnlCode), blank(svcCode), PENDING_LIMIT));
        return out;
    }

    /** 선택한 줄을 다시 보낸다. 같은 (주문코드·택배사·송장) 조합은 한 번만 호출하고 같은 결과를 나눠 준다. 결과 키는 "주문번호-줄-변경". */
    public Map<String, Result> resend(List<int[]> keys, AuthSession.Actor actor) {
        Map<String, Result> out = new LinkedHashMap<>();
        Map<String, Result> byCall = new LinkedHashMap<>();
        for (int[] k : keys) {
            String key = k[0] + "-" + k[1] + "-" + k[2];
            SlnkInvoiceLine line = mapper.selectLine(k[0], k[1], k[2]);
            if (line == null) { out.put(key, new Result(SKIP, "줄을 찾을 수 없습니다.")); continue; }
            String callKey = line.getSlnkOrderCode() + "|" + deliveryName(line) + "|" + line.getInvNo();
            Result r = byCall.get(callKey);
            if (r == null) {
                r = push(line, actor);
                byCall.put(callKey, r);
            }
            out.put(key, r);
        }
        return out;
    }

    // ── 이력 ─────────────────────────────────────────────────

    /** 이력 한 건 기록. 기록이 실패해도 전송 결과는 그대로 돌려준다(서버 로그에 남긴다). */
    private void record(SlnkInvoiceLine line, String deliveryName, String resultType, String message, String logUrl, AuthSession.Actor actor) {
        try {
            SlinkLog l = new SlinkLog();
            l.setLogType("송장");
            l.setOrderNo(line.getOrderNo());
            l.setOrderProdNo(line.getOrderProdNo());
            l.setOrderChangeNo(line.getOrderChangeNo());
            l.setDeliCompName(deliveryName);
            l.setInvNo(line.getInvNo());
            l.setResultType(resultType);
            l.setResultMessag(message == null ? null : message.length() > RESULT_MESSAGE_MAX ? message.substring(0, RESULT_MESSAGE_MAX) : message);
            l.setCallUrl(logUrl);   // 계정 ID 는 이미 가려진 주소
            l.setRegistId(actor == null ? null : actor.loginId());
            l.setRegistName(actor == null ? null : actor.name());
            logMapper.insert(l);
        } catch (RuntimeException e) {
            log.error("샵링커 송장 이력 기록 실패 orderNo={} resultType={}: {}", line.getOrderNo(), resultType, e.toString());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
