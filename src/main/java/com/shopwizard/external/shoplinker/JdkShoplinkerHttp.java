package com.shopwizard.external.shoplinker;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@link ShoplinkerHttp} 의 JDK {@code HttpClient} 구현(주문 수집 {@code ShoplinkerCollectService} 와 같은 방식). */
@Component
public class JdkShoplinkerHttp implements ShoplinkerHttp {

    private static final Pattern CHARSET = Pattern.compile("charset\\s*=\\s*\"?([\\w.:-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String get(String url, int timeoutSec) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSec, 30)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .GET()
                .build();
        HttpResponse<byte[]> resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("샵링커 응답 오류 HTTP " + resp.statusCode());
        }
        return decode(resp.body(), resp.headers().firstValue("Content-Type").orElse(null));
    }

    /**
     * 응답 본문을 문자열로. 2026-11 실전송 시험에서 샵링커 응답({@code false - 제조업체 누락 [에러코드 : 0005 …]})은 EUC-KR 로
     * 읽어도 정상이었다. 다만 인코딩을 하나로 단정하지 않고 ① 응답 헤더의 charset 이 있으면 그것, ② 없으면 엄격한 UTF-8,
     * ③ UTF-8 이 아니면 EUC-KR 순으로 읽는다 — 어느 쪽이든 깨진 문자로 조용히 대체하지 않는다.
     * (시험 중 메시지가 깨져 보였던 것은 개발 도구 콘솔의 표시 문제였고 저장된 값은 정상이었다.)
     */
    static String decode(byte[] body, String contentType) {
        if (body == null || body.length == 0) return "";
        Charset declared = declaredCharset(contentType);
        if (declared != null) {
            String s = strict(body, declared);
            if (s != null) return s;
        }
        String utf8 = strict(body, StandardCharsets.UTF_8);
        if (utf8 != null) return utf8;
        String euc = strict(body, ShoplinkerProductRequest.EUC_KR);
        if (euc != null) return euc;
        return new String(body, StandardCharsets.UTF_8);   // 어느 쪽도 아니면 최후 수단(대체 문자 포함)
    }

    private static Charset declaredCharset(String contentType) {
        if (contentType == null) return null;
        Matcher m = CHARSET.matcher(contentType);
        if (!m.find()) return null;
        try {
            return Charset.forName(m.group(1).toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String strict(byte[] body, Charset cs) {
        try {
            return cs.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(body)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
