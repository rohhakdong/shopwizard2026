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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 발주서 엑셀(.xls) — 출고지(ShipPlace)마다 시트 하나, 열은 레거시 발주서와 같은 구성.
 * 입력 행은 {@code ShipAutoOrderMapper.selectSheetRows} 의 정렬(출고지, 출력순서, 합배송, 수신자, 상품명)을 그대로 따른다.
 * 합배송 묶음은 두 가지 배경색을 번갈아 칠해 창고에서 같은 수신자 묶음을 한눈에 보게 한다. 사은품 행은 구분 열에 표시한다.
 * 공급단가/공급소계는 레거시 xls 가 "0" 을 박아 두었지만(화면 조회 xml 은 계산) 계산해서 채운다: 소계 = BuyPrice, 단가 = 소계 / 수량.
 */
public final class ShipOrderSheetExcel {

    static final String[] HEADERS = {"주문번호", "구분", "채널주문번호", "주문일", "주문자", "상품코드", "상품명", "상품속성명", "수량", "금액",
            "수령인", "전화", "핸드폰", "우편번호", "주소", "주문메모", "전달메모", "공급단가", "공급소계", "배송비", "배송유형", "채널명", "합배송"};
    private static final int[] WIDTHS = {12, 8, 18, 12, 10, 14, 34, 14, 6, 10, 10, 14, 14, 9, 40, 24, 24, 10, 10, 8, 8, 14, 7};
    /** .xls 한 시트의 행 한계(65,536)보다 작게 끊는다. */
    static final int MAX_ROWS_PER_SHEET = 60000;

    private ShipOrderSheetExcel() {}

    public static byte[] build(List<Map<String, Object>> rows) {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle bundleA = fill(wb, IndexedColors.LIGHT_YELLOW);
            CellStyle bundleB = fill(wb, IndexedColors.LIGHT_CORNFLOWER_BLUE);
            CellStyle gift = wb.createCellStyle();
            Font italic = wb.createFont();
            italic.setItalic(true);
            gift.setFont(italic);

            Map<String, List<Map<String, Object>>> byPlace = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                byPlace.computeIfAbsent(str(r.get("shipPlace")).isEmpty() ? "기본출고지" : str(r.get("shipPlace")), k -> new java.util.ArrayList<>()).add(r);
            }
            if (byPlace.isEmpty()) byPlace.put("기본출고지", List.of());

            Set<String> usedNames = new HashSet<>();
            for (Map.Entry<String, List<Map<String, Object>>> e : byPlace.entrySet()) {
                List<Map<String, Object>> list = e.getValue();
                int parts = Math.max(1, (list.size() + MAX_ROWS_PER_SHEET - 1) / MAX_ROWS_PER_SHEET);
                for (int p = 0; p < parts; p++) {
                    String base = parts == 1 ? e.getKey() : e.getKey() + " (" + (p + 1) + ")";
                    Sheet sheet = wb.createSheet(uniqueName(sheetName(base), usedNames));
                    writeHeader(sheet, head);
                    writeRows(sheet, list.subList(p * MAX_ROWS_PER_SHEET, Math.min(list.size(), (p + 1) * MAX_ROWS_PER_SHEET)),
                            bundleA, bundleB, gift);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("발주서 엑셀을 만들 수 없습니다.", ex);
        }
    }

    private static void writeHeader(Sheet sheet, CellStyle head) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(HEADERS[i]);
            c.setCellStyle(head);
            sheet.setColumnWidth(i, WIDTHS[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void writeRows(Sheet sheet, List<Map<String, Object>> list, CellStyle a, CellStyle b, CellStyle gift) {
        int rowNo = 1;
        String lastGroup = null;
        boolean toggle = false;
        for (Map<String, Object> r : list) {
            boolean bundled = num(r.get("bundleYn")) == 1;
            String group = bundled ? str(r.get("recverAddr")) + "@" + str(r.get("recverName")) : null;
            if (group != null && !group.equals(lastGroup)) toggle = !toggle;
            lastGroup = group;
            CellStyle style = group == null ? null : (toggle ? a : b);
            boolean isGift = "사은품".equals(str(r.get("orderType")));

            long qty = num(r.get("prodQty"));
            long buy = num(r.get("buyPrice"));
            Object[] values = {
                    r.get("orderNo"), isGift ? "사은품" : "", r.get("chnlOrderNo"), day(str(r.get("orderReciptDate"))), r.get("orderName"),
                    r.get("prodCode"), r.get("prodName"), r.get("itemName"), qty,
                    num(r.get("salePrice")) + num(r.get("nvPrice")),
                    r.get("recverName"), r.get("recverPhoneNo"), r.get("recverMobileNo"), r.get("recverZipcode"), r.get("recverAddr"),
                    r.get("orderMemo"), r.get("deliMemo"),
                    qty > 0 ? buy / qty : 0, buy, num(r.get("deliFeeAmt")), r.get("deliFeeType"), r.get("chnlName"),
                    bundled ? "합배송" : ""};
            Row row = sheet.createRow(rowNo++);
            for (int i = 0; i < values.length; i++) {
                Object v = values[i];
                Cell c = row.createCell(i);
                if (v instanceof Number n) c.setCellValue(n.doubleValue());
                else if (v != null && !String.valueOf(v).isEmpty()) c.setCellValue(String.valueOf(v));
                if (style != null) c.setCellStyle(style);
                else if (isGift) c.setCellStyle(gift);
            }
        }
    }

    private static CellStyle fill(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    /** 시트 이름 규칙: 31자 이하, [ ] : * ? / \ 금지. */
    static String sheetName(String raw) {
        String n = raw.replaceAll("[\\[\\]:*?/\\\\]", "_").trim();
        if (n.isEmpty()) n = "기본출고지";
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    private static String uniqueName(String name, Set<String> used) {
        String candidate = name;
        int i = 2;
        while (!used.add(candidate.toLowerCase())) {
            String suffix = "_" + i++;
            candidate = (name.length() + suffix.length() > 31 ? name.substring(0, 31 - suffix.length()) : name) + suffix;
        }
        return candidate;
    }

    private static String day(String dt) { return dt.length() >= 10 ? dt.substring(0, 10) : dt; }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static long num(Object o) { return o instanceof Number n ? n.longValue() : 0; }
}
