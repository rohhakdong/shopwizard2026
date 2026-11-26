package com.shopwizard.ship.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 발주내역·발주내역2 한 행의 계산값 — 화면(JSON)과 엑셀이 같은 값을 쓰도록 한 곳에서 만든다.
 * 레거시 simpleGridView 의 계산을 그대로 따르되 널·0 나눗셈에서 예외가 나지 않게 한다.
 */
final class ShipPrintRows {

    private ShipPrintRows() {}

    /** 발주내역 한 행. 금액 = 판매가 + 옵션금액, 공급단가 = 공급소계 / 수량(수량 0 이면 소계). */
    static Map<String, Object> print1(Map<String, Object> r) {
        long qty = num(r.get("prodQty"));
        long buy = num(r.get("buyPrice"));
        Map<String, Object> o = new LinkedHashMap<>();
        copy(o, r, "orderNo", "orderProdNo", "orderChangeNo", "chnlOrderNo");
        o.put("orderReciptDate", day(str(r.get("orderReciptDate"))));
        copy(o, r, "orderName", "prodCode", "prodName", "itemName");
        o.put("prodQty", qty);
        o.put("amount", num(r.get("salePrice")) + num(r.get("nvPrice")));
        copy(o, r, "recverName", "recverPhoneNo", "recverMobileNo", "recverZipcode", "recverAddr", "orderMemo", "deliMemo");
        o.put("unitPrice", qty == 0 ? buy : buy / qty);
        o.put("subTotal", buy);
        o.put("deliFeeAmt", num(r.get("deliFeeAmt")));
        o.put("deliFeeType", ShipOrderSheetExcel.deliType(r.get("deliFeeType")));
        copy(o, r, "orderState", "shopName", "chnlName");
        o.put("shipCheckDate", str(r.get("shipCheckDate")));
        o.put("shipPlace", str(r.get("shipPlace")).isEmpty() ? "기본출고지" : str(r.get("shipPlace")));
        o.put("bundleYn", num(r.get("bundleYn")) == 1 ? 1 : 0);
        return o;
    }

    /**
     * 발주내역2 한 행. 주문순번 2 이상(한 주문의 둘째 상품부터)의 금액류는 0 — 주문 금액이 중복 합산되지 않게 하는 레거시 규칙.
     * 공급단가 = 공급소계 / 낱개수량 × 수량. 구성수량 = 낱개수량 / 수량(수량 0 이면 낱개수량).
     */
    static Map<String, Object> print2(Map<String, Object> r) {
        long qty = num(r.get("prodQty"));
        long orderQty = num(r.get("orderProdQty"));
        long buy = num(r.get("buyPrice"));
        boolean first = num(r.get("orderProdNo")) <= 1;
        String shipCheck = str(r.get("shipCheckDate"));
        Map<String, Object> o = new LinkedHashMap<>();
        copy(o, r, "orderNo", "orderProdNo", "orderChangeNo", "chnlOrderNo");
        o.put("orderReciptDate", day(str(r.get("orderReciptDate"))));
        o.put("shipMonth", shipCheck.length() >= 7 ? shipCheck.substring(5, 7) : "");
        o.put("shipDay", shipCheck.length() >= 10 ? shipCheck.substring(8, 10) : "");
        copy(o, r, "recverName", "orderState", "shopProdCode", "prodMemo01", "orderProdName", "prodName", "unitName", "itemName");
        o.put("compQty", orderQty == 0 ? qty : qty / orderQty);
        o.put("orderProdQty", orderQty);
        o.put("prodQty", qty);
        o.put("salePrice", first ? num(r.get("salePrice")) : 0);
        o.put("nvPrice", first ? num(r.get("nvPrice")) : 0);
        o.put("totalPrice", first ? num(r.get("salePrice")) + num(r.get("nvPrice")) : 0);
        o.put("supplyPrice", first ? num(r.get("supplyPrice")) : 0);
        o.put("unitPrice", qty == 0 ? buy : buy / qty * orderQty);
        o.put("subTotal", buy);
        o.put("deliFeeAmt", num(r.get("deliFeeAmt")));
        o.put("deliFeeType", ShipOrderSheetExcel.deliType(r.get("deliFeeType")));
        copy(o, r, "chnlName");
        o.put("bundleYn", num(r.get("bundleYn")) == 1 ? 1 : 0);
        o.put("recverAddr", str(r.get("recverAddr")));
        return o;
    }

    private static void copy(Map<String, Object> to, Map<String, Object> from, String... keys) {
        for (String k : keys) to.put(k, from.get(k));
    }

    static String day(String dt) { return dt.length() >= 10 ? dt.substring(0, 10) : dt; }
    static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    static long num(Object o) { return o instanceof Number n ? n.longValue() : 0; }
}
