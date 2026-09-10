package com.shopwizard.order.service;

import com.shopwizard.external.shoplinker.Order;
import com.shopwizard.framework.util.StringUtil;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.SlnkOrderMapper;
import com.shopwizard.order.mapper.SlnkTransDetailMapper;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.SlnkTransDetail;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import com.shopwizard.store.model.Chnl;
import com.shopwizard.store.service.ChnlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 샵링커 주문 1건을 샵피온 주문(tOrdOrder / tOrdOrderProd / tOrdOrderPay)으로 생성한다.
 * 레거시 {@code OrderServiceImpl#runInsertOrder} 이식.
 *
 * <p>수집 전체가 한 트랜잭션이면 500번째 건 실패가 앞의 499건을 되돌리므로,
 * 주문 생성/이력 기록을 각각 {@code REQUIRES_NEW} 로 분리해 건별로 커밋한다.</p>
 */
@Component
@RequiredArgsConstructor
public class ShoplinkerOrderWriter {

    private static final Logger log = LoggerFactory.getLogger(ShoplinkerOrderWriter.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 샵링커 수집 주문의 결제구분 코드 (tOrdOrderPay.PayType — "기타"). 레거시 상수. */
    private static final String PAY_TYPE_ETC = "D00000000000";

    private final OrderNoSeqMapper orderNoSeqMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final OrderPayService orderPayService;
    private final SlnkOrderMapper slnkOrderMapper;
    private final SlnkTransDetailMapper slnkTransDetailMapper;
    private final ChnlService chnlService;
    private final ProdService prodService;

    public enum Outcome { SUCCESS, DUPLICATE, SKIP, ERROR }

    public int countByCode(String code) {
        return slnkOrderMapper.countByCode(code);
    }

    /**
     * 무시 대상이면 사유 문자열, 아니면 null. 레거시 하드코딩 규칙 이식
     * (→ 추후 공통코드/설정 테이블로 외부화 후보).
     */
    public String skipReason(Order o) {
        String mall = nz(o.getMallcode());
        String name = nz(o.getGoodName());

        if (mall.equals("티몬")) {
            List<String> pbNames = List.of(
                    "01.행주(주방용) 1P", "02.행주(주방용) 24P(1box)",
                    "03.손걸레(거실용) 1P", "04.손걸레(거실용) 24P(1box)",
                    "05.탈부착형 물티슈캡",
                    "01-1.행주(주방용) 1P + 올가물티슈 증정이벤트 (한정수량)",
                    "03-1.손걸레(거실용) 1P + 올가물티슈 증정이벤트 (한정수량)");
            if (pbNames.contains(name) || name.contains("(PB_사입)")) {
                return "티몬 물티슈 PB 건이라서 무시했습니다.";
            }
        }
        if (mall.contains("옥션") || mall.contains("지마켓")) {
            String pre = mall.contains("옥션") ? "옥션" : "지마켓";
            if (name.contains("cr-B400"))  return pre + " 화장지 cr-B400 건이라서 무시했습니다.";
            if (name.contains("cr-G9174")) return pre + " 화장지 cr-G9174 건이라서 무시했습니다.";
            if (name.contains("/SW24"))    return pre + " 건전지 SW24 건이라서 무시했습니다.";
            if (name.contains("e-smile"))  return pre + " e-smile 건이라서 무시했습니다.";
            if (name.contains("ed01"))     return pre + " ed01 건이라서 무시했습니다.";
        }
        return null;
    }

    /**
     * 주문 생성. 성공 시 null, 예상된 업무 오류(채널 없음/수량 오류 등)면 사유 문자열을 반환한다.
     * 사유 문자열을 반환할 때는 DB를 건드리기 전에 반환하므로 롤백할 것이 없다.
     * 예기치 못한 예외는 그대로 던져 이 트랜잭션(REQUIRES_NEW)만 롤백된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String createOrder(Order o, String registId, String registName) {
        // --- 검증 (DB 쓰기 전) ---
        int qty;
        try {
            qty = Integer.parseInt(nz(o.getGoodQty()).replaceAll(",", ""));
        } catch (NumberFormatException e) {
            return "수량 정보가 없습니다. (goodQty=" + o.getGoodQty() + ")";
        }

        Map<String, Object> chnlParam = new HashMap<>();
        chnlParam.put("pSlnkMallCode", nz(o.getMallcode()));
        chnlParam.put("pLoginId", nz(o.getMallid()));
        Chnl chnl = chnlService.selectByShoplinkerMall(chnlParam);
        if (chnl == null) {
            return "샵링커 몰코드 [" + o.getMallcode() + "] 아이디 [" + o.getMallid() + "] 에 매핑된 채널이 없습니다.";
        }
        String chnlCode = resolveChnlCode(o, chnl.getChnlCode());

        String orderDate  = parseOrderDate(nz(o.getOrderdate()), nz(o.getMallcode()));
        String approvDate = parseApprovDate(nz(o.getUpdateDate()));

        // --- 주문번호 채번 ---
        OrderNoSeq seq = new OrderNoSeq();
        orderNoSeqMapper.insert(seq);
        int orderNo = seq.getSeq();

        // --- 주문 헤더 (tOrdOrder) ---
        com.shopwizard.order.model.Order order = new com.shopwizard.order.model.Order();
        order.setOrderNo(orderNo);
        order.setCustId(0);
        order.setLoginId(nz(o.getBuyerId()));
        order.setOrderName(nz(o.getBuyerName()));
        order.setOrderEmail(nz(o.getBuyerEmail()));
        order.setOrderPhoneNo(nz(o.getBuyerTel()));
        order.setOrderMobileNo(pickMobile(o));
        order.setOrderZipcode("");
        order.setOrderAddr1("");
        order.setOrderAddr2(nz(o.getTransPolicy()));   // 레거시: 주문자주소2 칸에 배송비 정책
        order.setRecverName(nz(o.getRcpName()));
        order.setRecverZipcode(nz(o.getRcpZip()));
        order.setRecverAddr1(nz(o.getRcpAddr()));
        order.setRecverAddr2("");
        order.setRecverPhoneNo(nz(o.getRcpTel()));
        order.setRecverMobileNo(nz(o.getRcpHp()));
        order.setMoneyUnit("KRW");
        order.setDeliMemo(nz(o.getTransSendmemo()).replaceAll("<!\\[CDATA\\[", "").replaceAll("]]>", ""));
        order.setOrderState("지불완료");
        order.setOrderReciptDate(orderDate);
        order.setPayCmpletDate(approvDate);
        order.setChnlCode(chnlCode);
        order.setRegistId(registId);
        order.setRegistName(registName);
        orderService.insert(order);

        // --- 결제 (tOrdOrderPay) ---
        OrderPay pay = new OrderPay();
        pay.setOrderNo(orderNo);
        pay.setPayNo(1);
        pay.setPayType(PAY_TYPE_ETC);
        pay.setPayAmt(0);
        pay.setPgCode(nz(o.getMallcode()));
        pay.setPgOrderNo(nz(o.getCode()));
        pay.setPgTradeNo(nz(o.getCode()));
        pay.setApprovNo(nz(o.getCode()));
        pay.setApprovDate(approvDate);
        pay.setReciptName(nz(o.getBuyerName()));
        pay.setRegistId(registId);
        pay.setRegistName(registName);
        orderPayService.insert(pay);

        // --- 주문 상품 (tOrdOrderProd) ---
        OrderProd op = new OrderProd();
        op.setOrderNo(orderNo);
        op.setOrderProdNo(1);
        op.setOrderState("지불완료");
        op.setState(1);
        op.setBundleYn(0);
        op.setShopProdCode(resolveShopProdCode(o));

        String prodName = nz(o.getGoodName())
                .replaceAll("＆amp;quot;", "").replaceAll("＆amp;#39;", "");
        op.setProdName(prodName);
        op.setItemName(resolveItemName(o));
        op.setProdQty(qty);

        int salePrice = parseAmt(o.getGoodSprice());
        int optionAmt = StringUtil.stripPrice(nz(o.getGoodOpt()));
        if (nz(o.getMallcode()).contains("11번가")) optionAmt += StringUtil.stripPrices(nz(o.getGoodOpt()));
        int supplyPrice = parseAmt(o.getGoodBprice());
        // 씨제이오쇼핑 채널은 샵링커 공급가에 부가세 10% 가산 (레거시)
        if ("1148523731Z1".equals(chnlCode)) supplyPrice += (int) Math.floor(supplyPrice * 0.1);
        int buyPrice = parseAmt(o.getGoodOribprice());
        // 이지웰: 옵션가가 판매가에 중복 포함되어 차감 (레거시)
        if (nz(o.getMallcode()).contains("이지웰")) salePrice -= optionAmt;

        op.setListPrice(salePrice);
        op.setSalePrice(salePrice);
        op.setNvPrice(optionAmt);
        op.setSupplyPrice(supplyPrice);
        op.setBuyPrice(buyPrice);
        op.setProdMargin(salePrice + optionAmt - buyPrice);
        op.setChnlMargin(salePrice + optionAmt - supplyPrice);
        op.setNetMargin(supplyPrice - buyPrice);
        op.setVatRate(0);
        op.setVatAmt(0);
        op.setDeliFeeType(resolveDeliFeeType(nz(o.getTransPolicy())));
        op.setDeliFeeAmt(0);
        op.setPromotFeeAmt(0);
        op.setProdMemo01(nz(o.getGoodAuctionno()));
        op.setChnlOrderNo(nz(o.getOrderno()));
        op.setSlnkOrderCode(nz(o.getCode()));
        op.setAccntId("0");
        op.setRegistId(registId);
        op.setRegistName(registName);

        // 상점상품코드로 실상품 매칭 → 공급사/상점/제조사/단위/세율/원가 채우기
        if (!op.getShopProdCode().isEmpty()) {
            Map<String, Object> pp = new HashMap<>();
            pp.put("pShopProdCode", op.getShopProdCode());
            Prod prod = prodService.selectByShopProdCode(pp);
            if (prod != null) {
                op.setProdCode(prod.getProdCode());
                op.setSupplyCode(prod.getSupplyCode());
                op.setSupplyName(prod.getSupplyName());
                op.setShopCode(prod.getShopCode());
                op.setShopName(prod.getShopName());
                op.setStoreCode(prod.getCateCode());
                op.setMakerName(prod.getMakerName());
                op.setModelName(prod.getModelName());
                op.setProdUnit(prod.getProdUnit());
                op.setProdImg(prod.getImgUrl());
                if (prod.getVatRate() != null && prod.getVatRate() > 0) {
                    op.setVatRate(prod.getVatRate());
                    op.setVatAmt((int) ((long) salePrice * 100 / (100 + prod.getVatRate())));
                }
                if (prod.getBuyPrice() != null) {
                    op.setBuyPrice(prod.getBuyPrice());
                    op.setProdMargin(salePrice + optionAmt - prod.getBuyPrice());
                    op.setNetMargin(supplyPrice - prod.getBuyPrice());
                }
            }
        }
        orderProdService.insert(op);

        // 재고 차감 (매칭된 경우만). updateSupplyQty 는 SupplyQty - #{qty} 이므로 양수 전달.
        if (op.getProdCode() != null && !op.getProdCode().isEmpty()) {
            try {
                Map<String, Object> dec = new HashMap<>();
                dec.put("prodCode", op.getProdCode());
                dec.put("qty", qty);
                prodService.updateSupplyQty(dec);
            } catch (Exception e) {
                log.warn("샵링커 수집 재고차감 실패 prodCode={} : {}", op.getProdCode(), e.getMessage());
            }
        }

        // --- 원본 스테이징 (tOrdOrderShoplinker) ---
        o.setShopionOrderNo(String.valueOf(orderNo));
        o.setRegistId(registId);
        o.setRegistName(registName);
        slnkOrderMapper.insert(o);

        return null;
    }

    /** 건별 결과 이력 1행. 항상 별도 커밋(주문 생성 성공/실패와 무관하게 남긴다). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDetail(int transId, int transDetailNo, String slnkOrderCode,
                            String resultType, String message, String registId, String registName) {
        SlnkTransDetail d = new SlnkTransDetail();
        d.setTransId(transId);
        d.setTransDetailNo(transDetailNo);
        d.setSlnkOrderCode(slnkOrderCode);
        d.setResultType(resultType);
        d.setResultMessag(cut(message, 512));
        d.setRegistId(registId);
        d.setRegistName(registName);
        slnkTransDetailMapper.insert(d);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static int parseAmt(String s) {
        try { return (int) Math.round(Double.parseDouble(nz(s).replaceAll(",", ""))); }
        catch (NumberFormatException e) { return 0; }
    }

    /** 주문자 휴대번호가 특수문자로 15자 초과면 수취인 휴대번호로 대체 (레거시). */
    private static String pickMobile(Order o) {
        String bh = nz(o.getBuyerHp());
        if (bh.length() > 15) {
            String rh = nz(o.getRcpHp());
            return rh.length() < 15 ? rh : bh.substring(0, 15);
        }
        return bh;
    }

    /** 위메프/특정 브랜드는 상품코드를 받아오지 않는다 (견적/매칭 오류 방지, 레거시). */
    private static String resolveShopProdCode(Order o) {
        String name = nz(o.getGoodName());
        if (name.contains("프로스펙스") || name.contains("피에르가르뎅") || name.contains("스케쳐스")
                || name.contains("[트라이]홈쇼핑") || name.contains("백세내의") || name.contains("바디와일드")) {
            return "";
        }
        if (nz(o.getMallcode()).contains("위메프")) return "";
        return nz(o.getGoodNo());
    }

    /** 이지웰/롯데닷컴의 "기본옵션" 류는 옵션명에 상품명을 넣는다 (레거시). */
    private static String resolveItemName(Order o) {
        String mall = nz(o.getMallcode());
        String opt  = nz(o.getGoodOpt());
        if (mall.contains("이지웰")) {
            return (opt.equals("기본옵션") || opt.equals(":^")) ? nz(o.getGoodName()) : opt;
        }
        if (mall.contains("(주)롯데닷컴")) {
            return opt.equals("null (추가선택 : null)") ? nz(o.getGoodName()) : opt;
        }
        return opt;
    }

    private static String resolveDeliFeeType(String transPolicy) {
        if (transPolicy.contains("무료")) return "0";            // 무료
        if (transPolicy.contains("선결제") || transPolicy.contains("선불")) return "1"; // 선불
        if (transPolicy.contains("착불")) return "2";            // 착불
        return "0";
    }

    /** 스마일배송/PI 채널 오버라이드 (레거시 하드코딩 — 특정 상품 키워드 + 몰/아이디). */
    private static String resolveChnlCode(Order o, String baseChnlCode) {
        String name = nz(o.getGoodName());
        String mall = nz(o.getMallcode());
        String id   = nz(o.getMallid());
        boolean smile = name.toLowerCase().contains("ed01")
                || name.toUpperCase().contains("SW24")
                || name.contains("cr-B400") || name.contains("cr-G9174") || name.contains("e-smile");
        if (smile) {
            if (mall.equals("지마켓") && id.equals("shopion1")) return "2208183676Z7";
            if (mall.equals("지마켓") && id.equals("shopion2")) return "2208183676Z9";
            if (mall.equals("지마켓") && id.equals("shopion3")) return "2208183676Z8";
            if (mall.equals("(주)옥션") && id.equals("shopion1")) return "2148602085Z5";
            if (mall.equals("(주)옥션") && id.equals("shopion2")) return "2148602085Z4";
            return baseChnlCode;
        }
        if (name.toUpperCase().contains("PI")) {
            if (mall.equals("11번가") && id.equals("wshopion1")) return "104863696Z13";
        }
        return baseChnlCode;
    }

    private static String parseApprovDate(String updateDate) {
        if (updateDate.length() == 14) {
            return updateDate.substring(0, 4) + "-" + updateDate.substring(4, 6) + "-" + updateDate.substring(6, 8)
                    + " " + updateDate.substring(8, 10) + ":" + updateDate.substring(10, 12) + ":" + updateDate.substring(12, 14);
        }
        return LocalDateTime.now().format(DT);
    }

    private static String parseOrderDate(String orderdate, String mallcode) {
        try {
            if (orderdate.length() == 14) {
                String ymd = orderdate.substring(0, 4) + "-" + orderdate.substring(4, 6) + "-" + orderdate.substring(6, 8);
                if (mallcode.equals("아이스타일24(주)")) return ymd + " 00:00:00";
                if (mallcode.equals("YES24")) {
                    return ymd + " " + orderdate.substring(10, 12) + ":" + orderdate.substring(12, 14) + ":00";
                }
                int hour = Integer.parseInt(orderdate.substring(8, 10));
                if (hour > 23) {
                    LocalDateTime d = LocalDateTime.parse(ymd + "T00:00:00").plusDays(1);
                    String hh = String.format("%02d", hour - 24);
                    return d.toLocalDate() + " " + hh + ":" + orderdate.substring(10, 12) + ":" + orderdate.substring(12, 14);
                }
                return ymd + " " + orderdate.substring(8, 10) + ":" + orderdate.substring(10, 12) + ":" + orderdate.substring(12, 14);
            }
            if (orderdate.length() > 7) {
                return orderdate.substring(0, 4) + "-" + orderdate.substring(4, 6) + "-" + orderdate.substring(6, 8) + " 00:00:00";
            }
        } catch (RuntimeException e) {
            log.warn("샵링커 주문일자 파싱 실패 orderdate={} : {}", orderdate, e.getMessage());
        }
        return LocalDateTime.now().format(DT);
    }
}
