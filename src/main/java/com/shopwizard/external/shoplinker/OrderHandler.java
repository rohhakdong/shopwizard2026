package com.shopwizard.external.shoplinker;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class OrderHandler extends DefaultHandler {

    private List<Order> orderList;
    private Order order;
    private StringBuilder content;

    public List<Order> getOrderList() {
        return orderList;
    }

    @Override
    public void startDocument() {
        orderList = new ArrayList<>();
        content = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        content = new StringBuilder();
        if ("item".equals(qName)) order = new Order();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = content.toString().trim();
        if (order == null) return;
        switch (qName) {
            case "code"        -> order.setCode(value);
            case "orderdate"   -> order.setOrderdate(value);
            case "mallcode"    -> order.setMallcode(value);
            case "stcode"      -> order.setStcode(value);
            case "orderno"     -> order.setOrderno(value);
            case "goodNo"      -> order.setGoodNo(value);
            case "goodname"    -> order.setGoodname(value);
            case "optionname"  -> order.setOptionname(value);
            case "optioncode"  -> order.setOptioncode(value);
            case "goodqty"     -> order.setGoodqty(parseInt(value));
            case "goodprice"   -> order.setGoodprice(parseInt(value));
            case "totalprice"  -> order.setTotalprice(parseInt(value));
            case "buyerName"   -> order.setBuyerName(value);
            case "buyerHp"     -> order.setBuyerHp(value);
            case "buyerEmail"  -> order.setBuyerEmail(value);
            case "rcpName"     -> order.setRcpName(value);
            case "rcpHp"       -> order.setRcpHp(value);
            case "rcpZip"      -> order.setRcpZip(value);
            case "rcpAddr1"    -> order.setRcpAddr1(value);
            case "rcpAddr2"    -> order.setRcpAddr2(value);
            case "transNo"     -> order.setTransNo(value);
            case "transCorp"   -> order.setTransCorp(value);
            case "paymentDt"   -> order.setPaymentDt(value);
            case "deliveryDt"  -> order.setDeliveryDt(value);
            case "orderStatus" -> order.setOrderStatus(value);
            case "remark"      -> order.setRemark(value);
            case "item"        -> { orderList.add(order); order = null; }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value.replaceAll(",", "")); } catch (Exception e) { return 0; }
    }
}
