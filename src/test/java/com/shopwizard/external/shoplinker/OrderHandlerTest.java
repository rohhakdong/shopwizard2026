package com.shopwizard.external.shoplinker;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 샵링커 협력사 API 응답(속성 기반 XML)이 {@link OrderHandler} 로 올바르게 파싱되는지 검증.
 * 레거시 shopwizard 프로젝트의 실제 응답 예시를 그대로 사용한다.
 */
class OrderHandlerTest {

    private static final String SAMPLE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <SHOPLINKER datetime="2013-10-15 17:56:43" source="shopion1" version="SHOPLINKER C2C ORDER MAPPING 1.0">
          <order code="0108548813">
            <basetext orderdate="20131014215025" mallcode="(주)현대홈쇼핑" mallid="hs005259"
                      stcode="발주확인" orderno="20131014128738-001-001-001" sellerid=""
                      paymethod="" receipt="" canceldesc="" change_yn="N" />
            <good no="A01014207A" slgoodno="prod21885986" auction_no="2015686900"
                  bprice="31920" ori_bprice="40918" sprice="39900"
                  name="[트라이]홈쇼핑 은나노 순면 트렁크 10종세트" opt="003(105/설명참조)" qty="2" />
            <buyer id="" name="손수미" tel="02-0000-3365" hp="" email="a@b.com" />
            <rcp name="고명수" tel="010-9142-3546" hp="010-1111-2222" email="" zip="369-806" addr="충북 음성군 음성읍" />
            <trans policy="쇼핑몰확인요망" name="" no="" sendmemo="배송 전에 미리 연락 바랍니다." />
            <update date="20131015175643" />
          </order>
          <order code="0108548814">
            <basetext orderdate="20131014220000" mallcode="11번가" mallid="shopion1"
                      stcode="발주확인" orderno="ORD-2" change_yn="Y" />
            <good no="B0200000A" slgoodno="prd777" auction_no="333" bprice="1000" ori_bprice="900"
                  sprice="2000" name="테스트상품" opt="옵션1 (+1,000원)" qty="1" />
            <buyer id="buyer2" name="김철수" tel="" hp="010-3333-4444" email="" />
            <rcp name="김영희" tel="" hp="010-5555-6666" zip="12345" addr="서울시" />
            <trans policy="무료배송" name="CJ대한통운" no="" sendmemo="" />
            <update date="20131015180000" />
          </order>
        </SHOPLINKER>
        """;

    private List<Order> parse(String xml) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false);
        XMLReader xr = spf.newSAXParser().getXMLReader();
        OrderHandler h = new OrderHandler();
        xr.setContentHandler(h);
        xr.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        return h.getOrderList();
    }

    @Test
    void parsesAttributeBasedXml() throws Exception {
        List<Order> orders = parse(SAMPLE);

        assertThat(orders).hasSize(2);

        Order o1 = orders.get(0);
        assertThat(o1.getCode()).isEqualTo("0108548813");
        assertThat(o1.getMallcode()).isEqualTo("(주)현대홈쇼핑");
        assertThat(o1.getMallid()).isEqualTo("hs005259");
        assertThat(o1.getChangeyn()).isEqualTo("N");
        assertThat(o1.getGoodNo()).isEqualTo("A01014207A");
        assertThat(o1.getGoodSlgoodno()).isEqualTo("prod21885986");
        assertThat(o1.getGoodBprice()).isEqualTo("31920");
        assertThat(o1.getGoodOribprice()).isEqualTo("40918");
        assertThat(o1.getGoodSprice()).isEqualTo("39900");
        assertThat(o1.getGoodQty()).isEqualTo("2");
        assertThat(o1.getBuyerName()).isEqualTo("손수미");
        assertThat(o1.getBuyerEmail()).isEqualTo("a@b.com");
        assertThat(o1.getRcpName()).isEqualTo("고명수");
        assertThat(o1.getRcpZip()).isEqualTo("369-806");
        assertThat(o1.getRcpAddr()).isEqualTo("충북 음성군 음성읍");
        assertThat(o1.getTransPolicy()).isEqualTo("쇼핑몰확인요망");
        assertThat(o1.getTransSendmemo()).isEqualTo("배송 전에 미리 연락 바랍니다.");
        assertThat(o1.getUpdateDate()).isEqualTo("20131015175643");

        Order o2 = orders.get(1);
        assertThat(o2.getCode()).isEqualTo("0108548814");
        assertThat(o2.getMallcode()).isEqualTo("11번가");
        assertThat(o2.getChangeyn()).isEqualTo("Y");
        assertThat(o2.getGoodOpt()).isEqualTo("옵션1 (+1,000원)");
        assertThat(o2.getTransPolicy()).isEqualTo("무료배송");
    }

    @Test
    void emptyDocumentYieldsEmptyList() throws Exception {
        List<Order> orders = parse("<?xml version=\"1.0\"?><SHOPLINKER></SHOPLINKER>");
        assertThat(orders).isEmpty();
    }
}
