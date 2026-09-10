package com.shopwizard.external.shoplinker;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 샵링커 협력사 주문수집 API 응답(속성 기반 XML)을 SAX로 파싱한다.
 * 레거시 {@code com.shopion.external.shoplinker.OrderHandler} 를 그대로 이식.
 *
 * 하나의 {@code <order>} 요소가 {@link Order} 1건이 된다.
 */
public class OrderHandler extends DefaultHandler {

    private Order order;
    private List<Order> list;

    /** 파싱 결과. {@code startDocument} 전에는 null. */
    public List<Order> getOrderList() {
        return list;
    }

    @Override
    public void startDocument() {
        this.list = new ArrayList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        switch (qName) {
            case "order" -> {
                order = new Order();
                order.setCode(nz(atts.getValue("code")));
            }
            case "basetext" -> {
                if (order == null) break;
                order.setOrderdate(nz(atts.getValue("orderdate")));
                order.setMallcode(nz(atts.getValue("mallcode")));
                order.setMallid(nz(atts.getValue("mallid")));
                order.setStcode(nz(atts.getValue("stcode")));
                order.setOrderno(nz(atts.getValue("orderno")));
                order.setSellerid(nz(atts.getValue("sellerid")));
                order.setPaymethod(nz(atts.getValue("paymethod")));
                order.setReceipt(nz(atts.getValue("receipt")));
                order.setCanceldesc(nz(atts.getValue("canceldesc")));
                order.setChangeyn(nz(atts.getValue("change_yn")));
                // 하프클럽: orderdate가 비면 주문번호 앞 6자리(yyMMdd)로 대체
                if ("하프클럽".equals(order.getMallcode()) && order.getOrderdate().isEmpty()
                        && order.getOrderno().length() >= 6) {
                    order.setOrderdate("20" + order.getOrderno().substring(0, 6));
                }
            }
            case "good" -> {
                if (order == null) break;
                order.setGoodNo(nz(atts.getValue("no")));
                order.setGoodSlgoodno(nz(atts.getValue("slgoodno")));
                order.setGoodAuctionno(nz(atts.getValue("auction_no")));
                order.setGoodBprice(nz(atts.getValue("bprice")));
                order.setGoodOribprice(nz(atts.getValue("ori_bprice")));
                order.setGoodSprice(nz(atts.getValue("sprice")));
                order.setGoodName(nz(atts.getValue("name")));
                order.setGoodOpt(nz(atts.getValue("opt")));
                order.setGoodQty(nz(atts.getValue("qty")));
            }
            case "buyer" -> {
                if (order == null) break;
                order.setBuyerId(nz(atts.getValue("id")));
                order.setBuyerName(nz(atts.getValue("name")));
                order.setBuyerTel(nz(atts.getValue("tel")));
                order.setBuyerHp(nz(atts.getValue("hp")));
                order.setBuyerEmail(nz(atts.getValue("email")));
                // CJ몰(협력사) + 주문자명 "없음" → CJ몰(직택배)로 보정
                if ("CJ몰(협력사)".equals(order.getMallcode()) && "없음".equals(order.getBuyerName())) {
                    order.setMallcode("CJ몰(직택배)");
                }
            }
            case "rcp" -> {
                if (order == null) break;
                order.setRcpName(nz(atts.getValue("name")));
                order.setRcpTel(nz(atts.getValue("tel")));
                order.setRcpHp(nz(atts.getValue("hp")));
                order.setRcpEmail(nz(atts.getValue("email")));
                order.setRcpZip(nz(atts.getValue("zip")));
                order.setRcpAddr(nz(atts.getValue("addr")));
            }
            case "trans" -> {
                if (order == null) break;
                order.setTransPolicy(nz(atts.getValue("policy")));
                order.setTransName(nz(atts.getValue("name")));
                order.setTransNo(nz(atts.getValue("no")));
                order.setTransSendmemo(nz(atts.getValue("sendmemo")));
            }
            case "update" -> {
                if (order == null) break;
                order.setUpdateDate(nz(atts.getValue("date")));
            }
            default -> { /* SHOPLINKER 등 상위 요소는 무시 */ }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("order".equals(qName) && order != null) {
            list.add(order);
            order = null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
