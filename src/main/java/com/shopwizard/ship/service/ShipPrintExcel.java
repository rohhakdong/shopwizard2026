package com.shopwizard.ship.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 발주내역·발주내역2 엑셀(.xls). 입력은 {@link ShipPrintRows} 가 만든 행이라 화면과 같은 값이다.
 * 발주내역은 출고지마다 시트 하나, 발주내역2 는 상점명 시트 하나(60,000행을 넘으면 "(2)" 시트로 이어 감).
 * 합배송 묶음(같은 주소·수령인)은 두 가지 배경색을 번갈아 칠한다.
 */
final class ShipPrintExcel {

    /** 한 열의 정의: 머리글, 너비(글자 수), 값 꺼내기, 숫자 여부. */
    private record Col(String header, int width, Function<Map<String, Object>, Object> get) {}

    private static final List<Col> PRINT1 = List.of(
            col("주문번호", 12, "orderNo"), col("채널주문번호", 18, "chnlOrderNo"), col("주문일자", 12, "orderReciptDate"),
            col("주문자", 10, "orderName"), col("상품코드", 14, "prodCode"), col("상품명", 34, "prodName"), col("속성명", 14, "itemName"),
            col("수량", 6, "prodQty"), col("금액", 10, "amount"),
            col("수령인", 10, "recverName"), col("전화", 14, "recverPhoneNo"), col("핸드폰", 14, "recverMobileNo"),
            col("우편번호", 9, "recverZipcode"), col("주소", 40, "recverAddr"), col("주문메모", 24, "orderMemo"), col("전달메모", 24, "deliMemo"),
            col("공급단가", 10, "unitPrice"), col("공급소계", 10, "subTotal"), col("배송비", 8, "deliFeeAmt"), col("배송유형", 8, "deliFeeType"),
            col("주문상태", 10, "orderState"), col("공급자명", 14, "shopName"), col("채널명", 14, "chnlName"), col("발주일자", 18, "shipCheckDate"),
            new Col("합배송", 7, r -> ShipPrintRows.num(r.get("bundleYn")) == 1 ? "합배송" : ""));

    private static final List<Col> PRINT2 = List.of(
            col("주문번호", 12, "orderNo"), col("채널주문번호", 18, "chnlOrderNo"), col("주문일자", 12, "orderReciptDate"),
            col("발주월", 6, "shipMonth"), col("일", 5, "shipDay"), col("수령인", 10, "recverName"), col("주문상태", 10, "orderState"),
            col("상품코드", 14, "shopProdCode"), col("채널상품번호", 16, "prodMemo01"), col("상품명", 34, "orderProdName"), col("속성명", 24, "prodName"),
            col("단량", 10, "unitName"), col("사은품", 24, "itemName"),
            col("구성수량", 8, "compQty"), col("수량", 6, "orderProdQty"), col("낱개수량", 8, "prodQty"),
            col("금액", 10, "salePrice"), col("옵션금액", 10, "nvPrice"), col("판매금액", 10, "totalPrice"), col("채널공급가", 10, "supplyPrice"),
            col("공급단가", 10, "unitPrice"), col("공급소계", 10, "subTotal"), col("배송비", 8, "deliFeeAmt"), col("배송유형", 8, "deliFeeType"),
            col("채널명", 14, "chnlName"),
            new Col("합배송", 7, r -> ShipPrintRows.num(r.get("bundleYn")) == 1 ? "합배송" : ""));

    private ShipPrintExcel() {}

    private static Col col(String header, int width, String key) {
        return new Col(header, width, r -> r.get(key));
    }

    /** 발주내역 — 출고지(shipPlace)별 시트. rows 는 출고지 순으로 정렬돼 있어야 시트 순서가 안정적이다. */
    static byte[] buildPrint1(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> byPlace = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String place = ShipPrintRows.str(r.get("shipPlace")).isEmpty() ? "기본출고지" : ShipPrintRows.str(r.get("shipPlace"));
            byPlace.computeIfAbsent(place, k -> new ArrayList<>()).add(r);
        }
        if (byPlace.isEmpty()) byPlace.put("기본출고지", List.of());
        return build(byPlace, PRINT1);
    }

    /** 발주내역2 — 시트 하나(이름은 상점명). */
    static byte[] buildPrint2(String sheetTitle, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> one = new LinkedHashMap<>();
        one.put(sheetTitle == null || sheetTitle.isBlank() ? "발주내역2" : sheetTitle, rows);
        return build(one, PRINT2);
    }

    private static byte[] build(Map<String, List<Map<String, Object>>> sheets, List<Col> cols) {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle bundleA = fill(wb, IndexedColors.LIGHT_YELLOW);
            CellStyle bundleB = fill(wb, IndexedColors.LIGHT_GREEN);

            Set<String> used = new HashSet<>();
            int max = ShipOrderSheetExcel.MAX_ROWS_PER_SHEET;
            for (Map.Entry<String, List<Map<String, Object>>> e : sheets.entrySet()) {
                List<Map<String, Object>> list = e.getValue();
                int parts = Math.max(1, (list.size() + max - 1) / max);
                for (int p = 0; p < parts; p++) {
                    String base = parts == 1 ? e.getKey() : e.getKey() + " (" + (p + 1) + ")";
                    Sheet sheet = wb.createSheet(ShipOrderSheetExcel.uniqueName(ShipOrderSheetExcel.sheetName(base), used));
                    Row header = sheet.createRow(0);
                    for (int i = 0; i < cols.size(); i++) {
                        Cell c = header.createCell(i);
                        c.setCellValue(cols.get(i).header());
                        c.setCellStyle(head);
                        sheet.setColumnWidth(i, cols.get(i).width() * 256);
                    }
                    sheet.createFreezePane(0, 1);
                    writeRows(sheet, list.subList(p * max, Math.min(list.size(), (p + 1) * max)), cols, bundleA, bundleB);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("엑셀을 만들 수 없습니다.", ex);
        }
    }

    private static void writeRows(Sheet sheet, List<Map<String, Object>> list, List<Col> cols, CellStyle a, CellStyle b) {
        int rowNo = 1;
        String lastGroup = null;
        boolean toggle = false;
        for (Map<String, Object> r : list) {
            boolean bundled = ShipPrintRows.num(r.get("bundleYn")) == 1;
            String group = bundled ? ShipPrintRows.str(r.get("recverAddr")) + "@" + ShipPrintRows.str(r.get("recverName")) : null;
            if (group != null && !group.equals(lastGroup)) toggle = !toggle;
            lastGroup = group;
            CellStyle style = group == null ? null : (toggle ? a : b);
            Row row = sheet.createRow(rowNo++);
            for (int i = 0; i < cols.size(); i++) {
                Object v = cols.get(i).get().apply(r);
                Cell c = row.createCell(i);
                if (v instanceof Number n) c.setCellValue(n.doubleValue());
                else if (v != null && !String.valueOf(v).isEmpty()) c.setCellValue(String.valueOf(v));
                if (style != null) c.setCellStyle(style);
            }
        }
    }

    private static CellStyle fill(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
