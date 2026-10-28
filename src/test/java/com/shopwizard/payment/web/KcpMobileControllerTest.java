package com.shopwizard.payment.web;

import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.service.CheckoutPricingService;
import com.shopwizard.payment.model.KcpMobileRegisterRequest;
import com.shopwizard.payment.service.KcpGateway;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** KCP 모바일: 거래등록 요청의 금액이 서버값인지, Ret_URL(EUC-KR POST) 처리가 안전한지. */
class KcpMobileControllerTest {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private PaymentGatewayRegistry registry;
    private CheckoutPricingService pricing;
    private KcpGateway kcp;
    private KcpProperties props;
    private KcpMobileController controller;

    @BeforeEach
    void setUp() {
        registry = mock(PaymentGatewayRegistry.class);
        pricing = mock(CheckoutPricingService.class);
        kcp = mock(KcpGateway.class);
        props = new KcpProperties();
        props.setSiteCd("T0000");
        when(registry.require("KCP")).thenReturn(kcp);
        controller = new KcpMobileController(registry, pricing, props);
    }

    private static KcpMobileRegisterRequest req() {
        KcpMobileRegisterRequest r = new KcpMobileRegisterRequest();
        r.setOrderId("SW-1");
        r.setPayMethod("card");
        r.setOrderName("상품");
        CheckoutProdItem it = new CheckoutProdItem();
        it.setProdCode("P1");
        it.setProdQty(1);
        r.setOrderProds(List.of(it));
        return r;
    }

    @Test
    @SuppressWarnings("unchecked")
    void 거래등록_금액은_서버_가격계산_합계이고_결제창_폼을_만들어_준다() {
        PricedOrder priced = new PricedOrder();
        priced.setTotalAmt(26900);
        when(pricing.price(org.mockito.ArgumentMatchers.anyList())).thenReturn(priced);
        when(kcp.registerMobile(eq("SW-1"), eq(26900L), eq("상품"), eq("card"), anyString()))
                .thenReturn(new KcpGateway.MobileRegistration("AK-1", "https://testsmpay.kcp.co.kr/pay/mobileGW.kcp", "CARD"));
        props.setPublicBaseUrl("https://shop.example.com/");

        Map<String, Object> out = controller.register(req(), new MockHttpServletRequest());

        assertThat(out.get("action")).isEqualTo("https://testsmpay.kcp.co.kr/pay/jsp/encodingFilter/encodingFilter.jsp");
        Map<String, String> f = (Map<String, String>) out.get("fields");
        assertThat(f).containsEntry("site_cd", "T0000").containsEntry("pay_method", "CARD").containsEntry("currency", "410")
                .containsEntry("approval_key", "AK-1").containsEntry("ordr_idxx", "SW-1").containsEntry("good_mny", "26900")
                .containsEntry("Ret_URL", "https://shop.example.com/payment/kcp/return")
                .containsEntry("PayUrl", "https://testsmpay.kcp.co.kr/pay/mobileGW.kcp");
        // 거래등록에 쓴 Ret_URL 과 결제창 폼의 Ret_URL 이 같아야 한다(KCP 요구).
        verify(kcp).registerMobile("SW-1", 26900L, "상품", "card", "https://shop.example.com/payment/kcp/return");
    }

    @Test
    void 공개주소를_설정하지_않으면_요청이_들어온_주소로_Ret_URL_을_만든다() {
        PricedOrder priced = new PricedOrder();
        priced.setTotalAmt(1000);
        when(pricing.price(org.mockito.ArgumentMatchers.anyList())).thenReturn(priced);
        when(kcp.registerMobile(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new KcpGateway.MobileRegistration("AK", "https://x/pay/a.kcp", "CARD"));
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setScheme("https");
        http.setServerName("shop.example.com");
        http.setServerPort(443);

        controller.register(req(), http);

        verify(kcp).registerMobile(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(),
                eq("https://shop.example.com/payment/kcp/return"));
    }

    @Test
    void 복귀_본문은_EUC_KR_로_해석한다() throws Exception {
        byte[] body = ("res_cd=0000&res_msg=" + java.net.URLEncoder.encode("정상처리", EUC_KR)
                + "&enc_data=AbC%2B%2F%3D&enc_info=XyZ&tran_cd=00100000&ordr_idxx=SW-1").getBytes(EUC_KR);

        Map<String, String> p = KcpMobileController.parseReturnBody(body, null);

        assertThat(p).containsEntry("res_cd", "0000").containsEntry("res_msg", "정상처리")
                .containsEntry("enc_data", "AbC+/=").containsEntry("tran_cd", "00100000").containsEntry("ordr_idxx", "SW-1");
    }

    @Test
    void 복귀_응답은_주문서로_돌려보내고_스크립트를_깨뜨릴_값은_이스케이프한다() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/payment/kcp/return");
        http.setContent(("res_cd=9999&res_msg=" + java.net.URLEncoder.encode("</script><img src=x onerror=alert(1)>", EUC_KR)
                + "&enc_data=A&evil=ignored").getBytes(EUC_KR));

        ResponseEntity<String> res = controller.kcpReturn(http);

        String html = res.getBody();
        assertThat(html).contains("sessionStorage.setItem('kcpReturn'").contains("location.replace(\"/shop.html?kcp=return\")");
        // 값 안의 </script> 가 그대로 들어가면 스크립트가 닫히고 주입된다.
        assertThat(html).doesNotContain("</script><img").contains("\\u003c/script\\u003e");
        // 주문서가 쓰지 않는 파라미터는 전달하지 않는다.
        assertThat(html).doesNotContain("evil");
        assertThat(res.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void 본문이_비어_있으면_컨테이너가_읽은_파라미터를_쓴다() {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/payment/kcp/return");
        http.setParameter("res_cd", "3001");

        assertThat(KcpMobileController.parseReturnBody(new byte[0], http)).containsEntry("res_cd", "3001");
    }

    @Test
    void 줄종결자와_앰퍼샌드도_이스케이프한다() {
        String json = KcpMobileController.scriptSafeJson(Map.of("a", "x\u2028y&z"));
        assertThat(json).contains("\\u2028").contains("\\u0026").doesNotContain("\u2028");
    }
}
