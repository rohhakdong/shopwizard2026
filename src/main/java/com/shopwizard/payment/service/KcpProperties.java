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

    public String scriptUrlResolved() {
        String s = scriptUrl == null ? "" : scriptUrl.trim();
        return s.isEmpty() ? (prod() ? PROD_SCRIPT : TEST_SCRIPT) : s;
    }
}
