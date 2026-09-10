package com.shopwizard.external.shoplinker;

import lombok.Data;

/**
 * 샵링커(ShopLinker) 협력사 주문수집 API가 내려주는 주문 1건(= 주문상품 1줄).
 *
 * 실제 응답 XML은 <b>속성(attribute) 기반</b>이다 (레거시 shopwizard 프로젝트와 동일):
 * <pre>
 * &lt;SHOPLINKER datetime="..." source="shopion1" version="SHOPLINKER C2C ORDER MAPPING 1.0"&gt;
 *   &lt;order code="0108548813"&gt;
 *     &lt;basetext orderdate="20131014215025" mallcode="(주)현대홈쇼핑" mallid="hs005259"
 *               stcode="발주확인" orderno="20131014128738-001-001-001" sellerid=""
 *               paymethod="" receipt="" canceldesc="" change_yn="N" /&gt;
 *     &lt;good no="A01014207A" slgoodno="prod21885986" auction_no="2015686900"
 *           bprice="31920" ori_bprice="40918" sprice="39900" name="..." opt="003(105/설명참조)" qty="1" /&gt;
 *     &lt;buyer id="" name="손수미" tel="02-0000-3365" hp="" email="" /&gt;
 *     &lt;rcp name="고명*" tel="010-9142-3546" hp="" email="" zip="369-806" addr="충북 음성군 ..." /&gt;
 *     &lt;trans policy="쇼핑몰확인요망" name="" no="" sendmemo="배송 전에 미리 연락 바랍니다." /&gt;
 *     &lt;update date="20131015175643" /&gt;
 *   &lt;/order&gt;
 * &lt;/SHOPLINKER&gt;
 * </pre>
 *
 * {@link OrderHandler} 가 이 형식을 SAX로 파싱한다.
 */
@Data
public class Order {
    private String code;          // 주문코드 (tOrdOrderShoplinker PK, 중복 판단 기준)
    private String orderdate;     // 주문일자 (yyyyMMddHHmmss 등)
    private String mallcode;      // 제휴몰명 (예: "(주)현대홈쇼핑")
    private String mallid;        // 제휴몰 아이디
    private String stcode;        // 상태
    private String orderno;       // 채널 주문번호
    private String sellerid;      // 판매자 아이디
    private String paymethod;     // 지불방법
    private String receipt;       // 접수
    private String canceldesc;    // 취소사유
    private String changeyn;      // 교환여부 (Y/N)
    private String goodNo;        // 상품코드 (샵피온 내부 ShopProdCode 매칭용)
    private String goodSlgoodno;  // 샵링커 상품코드
    private String goodAuctionno; // 경매번호(채널상품코드)
    private String goodBprice;    // 공급가격1
    private String goodOribprice; // 공급가격2(원가)
    private String goodSprice;    // 판매가격
    private String goodName;      // 상품명칭
    private String goodOpt;       // 상품옵션
    private String goodQty;       // 주문수량
    private String buyerId;       // 주문자 아이디
    private String buyerName;     // 주문자
    private String buyerTel;      // 주문자 전화번호
    private String buyerHp;       // 주문자 휴대폰
    private String buyerEmail;    // 주문자 이메일
    private String rcpName;       // 수취인
    private String rcpTel;        // 수취인 전화번호
    private String rcpHp;         // 수취인 휴대폰
    private String rcpEmail;      // 수취인 이메일
    private String rcpZip;        // 수취인 우편번호
    private String rcpAddr;       // 수취인 주소
    private String transPolicy;   // 배송비 정책
    private String transName;     // 택배사
    private String transNo;       // 송장번호
    private String transSendmemo; // 배송메모
    private String updateDate;    // 수정일자 (승인일시로 사용)
    private String shopionOrderNo;// 생성된 샵피온 주문번호 (역참조용, 파싱 후 채움)

    // 수집을 실행한 관리자 (XML에는 없고 서비스에서 채운다)
    private String registId;
    private String registName;
}
