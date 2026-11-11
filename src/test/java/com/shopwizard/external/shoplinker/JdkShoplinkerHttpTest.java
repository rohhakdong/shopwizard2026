package com.shopwizard.external.shoplinker;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 샵링커 응답 디코딩 — 실전송 시험에서 UTF-8 응답을 EUC-KR 로 읽어 오류 메시지가 깨진 일을 막는다. */
class JdkShoplinkerHttpTest {

    private static final String MSG = "false - 상품이미지 누락 [에러코드 : 0007-1]";

    @Test
    void 헤더에_charset이_없으면_UTF_8로_읽는다() {
        assertEquals(MSG, JdkShoplinkerHttp.decode(MSG.getBytes(StandardCharsets.UTF_8), "text/html"));
        assertEquals(MSG, JdkShoplinkerHttp.decode(MSG.getBytes(StandardCharsets.UTF_8), null));
    }

    @Test
    void UTF_8이_아니면_EUC_KR로_읽는다() {
        byte[] euc = MSG.getBytes(ShoplinkerProductRequest.EUC_KR);
        assertEquals(MSG, JdkShoplinkerHttp.decode(euc, "text/html"));
    }

    @Test
    void 헤더가_선언한_charset을_먼저_따른다() {
        byte[] euc = MSG.getBytes(ShoplinkerProductRequest.EUC_KR);
        assertEquals(MSG, JdkShoplinkerHttp.decode(euc, "text/html; charset=EUC-KR"));
        assertEquals(MSG, JdkShoplinkerHttp.decode(MSG.getBytes(StandardCharsets.UTF_8), "text/html; charset=\"utf-8\""));
    }

    @Test
    void 헤더가_틀려도_본문이_그_인코딩이_아니면_다른_인코딩으로_읽는다() {
        // 헤더는 EUC-KR 이라는데 실제 본문은 UTF-8 — 깨진 글자로 대체하지 않고 UTF-8 로 읽는다
        assertEquals(MSG, JdkShoplinkerHttp.decode(MSG.getBytes(StandardCharsets.UTF_8), "text/html; charset=EUC-KR"));
    }

    @Test
    void 성공_응답과_빈_응답_알_수_없는_charset도_안전하다() {
        assertEquals("true|prd149987748|성공", JdkShoplinkerHttp.decode("true|prd149987748|성공".getBytes(StandardCharsets.UTF_8), "text/plain"));
        assertEquals("", JdkShoplinkerHttp.decode(new byte[0], "text/html"));
        assertEquals("", JdkShoplinkerHttp.decode(null, null));
        assertEquals(MSG, JdkShoplinkerHttp.decode(MSG.getBytes(StandardCharsets.UTF_8), "text/html; charset=no-such-charset"));
    }
}
