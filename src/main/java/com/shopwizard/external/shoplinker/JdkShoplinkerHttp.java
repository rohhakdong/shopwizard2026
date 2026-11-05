package com.shopwizard.external.shoplinker;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** {@link ShoplinkerHttp} 의 JDK {@code HttpClient} 구현(주문 수집 {@code ShoplinkerCollectService} 와 같은 방식). */
@Component
public class JdkShoplinkerHttp implements ShoplinkerHttp {

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
        return new String(resp.body(), ShoplinkerProductRequest.EUC_KR);
    }
}
