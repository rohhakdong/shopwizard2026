package com.shopwizard.payment.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * NHN KCP 연동 설정({@code kcp.*}). 인증서·개인키 <b>파일 자체는 저장소에 두지 않고</b> 경로만 받는다. 개인키 비밀번호는
 * 환경변수로만 주입하며 {@code toString} 이 없어(Getter/Setter 만) 로그에 찍히지 않는다.
 *
 * <ul>
 *   <li>{@code mode}: test(KCP 테스트 서버, 사이트코드 T0000 + 공용 샘플 인증서) | prod(운영 서버, 발급받은 사이트코드·인증서)</li>
 *   <li>{@code apiBase}/{@code scriptUrl}: 비워 두면 mode 에 맞는 KCP 주소를 쓴다(테스트에서 가짜 서버로 바꿀 때만 지정).</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kcp")
public class KcpProperties {

    public static final String TEST_API = "https://stg-spl.kcp.co.kr";
    public static final String PROD_API = "https://spl.kcp.co.kr";
    public static final String TEST_SCRIPT = "https://testspay.kcp.co.kr/plugin/kcp_spay_hub.js";
    public static final String PROD_SCRIPT = "https://spay.kcp.co.kr/plugin/kcp_spay_hub.js";
    /** 모바일 거래등록(/trade/register.do) 서버 — 승인 API 와 호스트가 다르다. */
    public static final String TEST_MOBILE_API = "https://testsmpay.kcp.co.kr";
    public static final String PROD_MOBILE_API = "https://smpay.kcp.co.kr";
    /** KCP 공용 테스트 사이트코드 — 운영 모드에서는 쓸 수 없다. */
    public static final String TEST_SITE_CD = "T0000";

    private String mode = "test";
    private String siteCd = "";
    private String siteName = "";
    private String certPath = "";
    private String privateKeyPath = "";
    private String privateKeyPassword = "";
    private String apiBase = "";
    private String scriptUrl = "";
    private String mobileApiBase = "";
    /** 모바일 결제 복귀(Ret_URL)의 기준 주소(예: https://shop.example.com). 비워 두면 요청이 들어온 주소를 쓴다. KCP 는 https 를 요구한다. */
    private String publicBaseUrl = "";
    /** 웹훅 hash_data 검증키(파트너관리자 > 웹훅관리). 환경변수로만 주입하고 로그에 남기지 않는다. */
    private String webhookKey = "";
    /** 웹훅 허용 발신 IP(쉼표 구분). 비면 mode 별 KCP 공개 IP. */
    private String webhookAllowedIps = "";
    /** 프록시/터널 뒤에서 X-Forwarded-For 첫 값을 발신 IP 로 쓸지 — 헤더는 위조될 수 있어 신뢰할 수 있는 프록시 뒤에서만 켠다. */
    private boolean webhookTrustForwarded = false;
    /** 가상계좌 입금 마감까지의 시간(분). 기본 72시간. */
    private int vaExpireMinutes = 4320;
    /** 마감 후 만료 처리까지 두는 여유(분) — 마감 직전 입금과의 경합을 피한다. */
    private int vaExpireGraceMinutes = 60;
    private boolean vaExpireJobEnabled = true;
    private int connectTimeoutMs = 5000;
    /** 승인 응답 대기. KCP 문서에 기준이 없어 넉넉히 잡고, 초과하면 거래 조회로 승인 여부를 확인한다. */
    private int readTimeoutMs = 30000;

    public boolean prod() {
        return "prod".equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    public String apiBaseUrl() {
        String b = apiBase == null ? "" : apiBase.trim();
        if (b.isEmpty()) return prod() ? PROD_API : TEST_API;
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    /** KCP 웹훅 발신 IP(문서): 테스트 1개, 운영 3개. */
    public static final String TEST_WEBHOOK_IPS = "210.122.176.144";
    public static final String PROD_WEBHOOK_IPS = "103.215.144.173,103.215.144.174,210.122.72.173";

    /** 웹훅을 받아도 되는 IP 집합 — 설정이 있으면 그것, 없으면 mode 별 KCP 공개 IP. */
    public java.util.Set<String> webhookIpSet() {
        String raw = webhookAllowedIps == null || webhookAllowedIps.isBlank()
                ? (prod() ? PROD_WEBHOOK_IPS : TEST_WEBHOOK_IPS) : webhookAllowedIps;
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String s : raw.split(",")) {
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    public String mobileApiBaseUrl() {
        String b = mobileApiBase == null ? "" : mobileApiBase.trim();
        if (b.isEmpty()) return prod() ? PROD_MOBILE_API : TEST_MOBILE_API;
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    /** 설정된 공개 주소(끝 슬래시 제거). 없으면 빈 문자열. */
    public String publicBase() {
        String b = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    public String scriptUrlResolved() {
        String s = scriptUrl == null ? "" : scriptUrl.trim();
        return s.isEmpty() ? (prod() ? PROD_SCRIPT : TEST_SCRIPT) : s;
    }
}
