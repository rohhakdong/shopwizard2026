package com.shopwizard.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KCP REST API(승인 / 취소 / 거래조회) 호출. 요청 본문 구성과 서명만 담당하고 응답 해석(res_cd 판단)은 {@link KcpGateway} 가 한다.
 * 응답에 {@code res_cd != 0000} 이 와도 예외가 아니라 그대로 돌려준다 — 예외({@link KcpException})는 호출 자체가 실패했을 때만.
 *
 * <p>KCP 는 거래조회를 초당 10회 이상 보내면 IP 를 차단할 수 있어, 모든 호출 사이에 최소 간격을 둔다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KcpClient {

    private static final long MIN_INTERVAL_MS = 150;

    private final KcpProperties props;
    private final KcpCredentials credentials;
    private final ObjectMapper mapper = new ObjectMapper();

    private long lastCallAt;

    /** 결제창 인증 결과(encData/encInfo/tranCd)로 승인. {@code ordrMony}/{@code payType}/{@code ordrNo} 는 서버 값 — KCP 가 인증된 값과 대조한다. */
    public Map<String, Object> approve(String encData, String encInfo, String tranCd,
                                       long ordrMony, String payType, String ordrNo) throws KcpException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site_cd", props.getSiteCd());
        body.put("kcp_cert_info", credentials.certInfo());
        body.put("enc_data", encData);
        body.put("enc_info", encInfo);
        body.put("tran_cd", tranCd);
        body.put("ordr_mony", String.valueOf(ordrMony));
        body.put("pay_type", payType);
        body.put("ordr_no", ordrNo);
        return post("/gw/enc/v1/payment", body, "approve");
    }

    /**
     * 가상계좌 발급(서버 API — 결제창 없음). 서명은 필요 없고 인증서만 보낸다. 성공 코드는 {@code V000}.
     * {@code vaDate} 는 입금 마감(yyyyMMddHHmmss), {@code vaName} 은 입금자명(14자 이내).
     */
    public Map<String, Object> issueVirtualAccount(String ordrIdxx, long amount, String goodName, String buyerName,
                                                   String bankCode, String vaName, String vaDate) throws KcpException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site_cd", props.getSiteCd());
        body.put("kcp_cert_info", credentials.certInfo());
        body.put("pay_method", "VCNT");
        body.put("amount", String.valueOf(amount));
        body.put("currency", "410");
        body.put("ordr_idxx", ordrIdxx);
        body.put("good_name", goodName);
        body.put("buyr_name", buyerName);
        body.put("va_txtype", "41100000");
        body.put("va_mny", String.valueOf(amount));
        body.put("va_bankcode", bankCode);
        body.put("va_name", vaName);
        body.put("va_date", vaDate);
        return post("/gw/hub/v1/payment", body, "vbank-issue");
    }

    /** 거래취소. {@code modType} STSC(전체)/STPC(부분 — modMny·remMny 필수). 가상계좌 사용중지도 STSC. */
    public Map<String, Object> cancel(String tno, String modType, Long modMny, Long remMny, String reason) throws KcpException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site_cd", props.getSiteCd());
        body.put("tno", tno);
        body.put("kcp_cert_info", credentials.certInfo());
        body.put("kcp_sign_data", credentials.sign(props.getSiteCd() + "^" + tno + "^" + modType));
        body.put("mod_type", modType);
        if ("STPC".equals(modType)) {
            body.put("mod_mny", String.valueOf(modMny));
            body.put("rem_mny", String.valueOf(remMny));
        }
        if (reason != null && !reason.isBlank()) body.put("mod_desc", reason.length() > 100 ? reason.substring(0, 100) : reason);
        return post("/gw/mod/v1/cancel", body, "cancel");
    }

    /** 거래번호(tno)로 조회. */
    public Map<String, Object> inquiryByTno(String tno, String payType) throws KcpException {
        return inquiry(tno, null, payType);
    }

    /** 가맹점 주문번호로 조회 — tno 자리에는 결제일(yyyyMMdd) + "000000" 을 넣는다. */
    public Map<String, Object> inquiryByOrder(String yyyymmdd, String ordrIdxx, String payType) throws KcpException {
        return inquiry(yyyymmdd + "000000", ordrIdxx, payType);
    }

    private Map<String, Object> inquiry(String tno, String modOrdrIdxx, String payType) throws KcpException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site_cd", props.getSiteCd());
        body.put("kcp_cert_info", credentials.certInfo());
        body.put("kcp_sign_data", credentials.sign(props.getSiteCd() + "^" + tno + "^" + payType));
        body.put("tno", tno);
        if (modOrdrIdxx != null) body.put("mod_ordr_idxx", modOrdrIdxx);
        body.put("pay_type", payType);
        return post("/std/inquery", body, "inquiry");
    }

    /**
     * 모바일 거래등록. 서명·인증서 없이 사이트코드만 보낸다. 응답은 {@code Code}(0000 정상)/{@code Message}/{@code approvalKey}/{@code PayUrl}.
     * 승인 API 와 다른 서버라 호출 간격 제한을 공유하지 않는다(공개 엔드포인트가 승인 호출을 지연시키지 않도록).
     */
    public Map<String, Object> registerTrade(String ordrIdxx, long goodMny, String goodName, String payMethod, String retUrl) throws KcpException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site_cd", props.getSiteCd());
        body.put("ordr_idxx", ordrIdxx);
        body.put("good_mny", String.valueOf(goodMny));
        body.put("good_name", goodName);
        body.put("pay_method", payMethod);
        body.put("Ret_URL", retUrl);
        return post(props.mobileApiBaseUrl(), "/trade/register.do", body, "register", false);
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String what) throws KcpException {
        return post(props.apiBaseUrl(), path, body, what, true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String base, String path, Map<String, Object> body, String what, boolean throttled) throws KcpException {
        if (throttled) throttle();
        HttpURLConnection conn;
        byte[] payload;
        try {
            payload = mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(base + path).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setConnectTimeout(props.getConnectTimeoutMs());
            conn.setReadTimeout(props.getReadTimeoutMs());
            conn.setDoOutput(true);
            conn.connect();
        } catch (IOException e) {
            // 연결 전 단계 — 요청이 KCP 에 도달하지 않았다.
            throw new KcpException("KCP 서버에 연결하지 못했습니다(" + what + ")", false, e);
        }
        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            String resp = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            Map<String, Object> parsed = (Map<String, Object>) mapper.readValue(resp, Map.class);
            log.info("KCP {} http={} res_cd={}", what, code, parsed.getOrDefault("res_cd", parsed.get("Code")));
            return parsed;
        } catch (IOException | RuntimeException e) {
            // 요청을 보낸 뒤의 실패(타임아웃·끊김·이해할 수 없는 응답) — KCP 가 처리했는지 알 수 없다.
            log.warn("KCP {} 응답을 받지 못했습니다: {}", what, e.getClass().getSimpleName());
            throw new KcpException("KCP 응답을 확인하지 못했습니다(" + what + ")", true, e);
        } finally {
            conn.disconnect();
        }
    }

    private synchronized void throttle() {
        long wait = lastCallAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "{}";
        try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
