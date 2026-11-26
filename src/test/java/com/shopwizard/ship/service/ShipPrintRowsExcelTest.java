package com.shopwizard.ship.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 발주내역·발주내역2 행 계산(공급단가·금액류·널 안전)과 엑셀(시트 분리·열·합배송 음영). */
class ShipPrintRowsExcelTest {

    private static Map<String, Object> base() {
        Map<String, Object> r = new HashMap<>();
        r.put("orderNo", 100);
        r.put("orderProdNo", 1);
        r.put("orderChangeNo", 1);
        r.put("chnlOrderNo", "CH-100");
        r.put("orderReciptDate", "2026-11-20 10:11:12");
        r.put("shipCheckDate", "2026-11-21 09:00:00");
        r.put("orderName", "주문자");
        r.put("prodCode", "P1");
        r.put("prodName", "상품");
        r.put("itemName", "");
        r.put("prodQty", 4);
        r.put("orderProdQty", 2);
        r.put("salePrice", 10000);
        r.put("nvPrice", 500);
        r.put("supplyPrice", 8000);
        r.put("buyPrice", 6000);
        r.put("recverName", "홍길동");
        r.put("recverAddr", "서울 어딘가");
        r.put("deliFeeAmt", 3000);
        r.put("deliFeeType", "1");
        r.put("chnlName", "쿠팡");
        r.put("shipPlace", "보물유통");
        r.put("bundleYn", 0);
        return r;
    }

    // ── 행 계산 ──────────────────────────────────────────────

    @Test
    void 발주내역은_금액을_판매가_더하기_옵션금액으로_공급단가를_소계_나누기_수량으로_계산한다() {
        Map<String, Object> o = ShipPrintRows.print1(base());

        assertThat(o.get("amount")).isEqualTo(10500L);
        assertThat(o.get("unitPrice")).isEqualTo(1500L);   // 6000 / 4
        assertThat(o.get("subTotal")).isEqualTo(6000L);
        assertThat(o.get("orderReciptDate")).isEqualTo("2026-11-20");
        assertThat(o.get("deliFeeType")).isEqualTo("선불");
    }

    @Test
    void 발주내역은_수량_0이면_공급단가가_소계이고_출고지가_비면_기본출고지다() {
        Map<String, Object> r = base();
        r.put("prodQty", 0);
        r.put("shipPlace", null);
        Map<String, Object> o = ShipPrintRows.print1(r);

        assertThat(o.get("unitPrice")).isEqualTo(6000L);
        assertThat(o.get("shipPlace")).isEqualTo("기본출고지");
    }

    @Test
    void 발주내역2는_첫_상품_행만_금액을_싣고_공급단가는_소계_나누기_낱개수량_곱하기_수량이다() {
        Map<String, Object> first = ShipPrintRows.print2(base());
        assertThat(first.get("salePrice")).isEqualTo(10000L);
        assertThat(first.get("nvPrice")).isEqualTo(500L);
        assertThat(first.get("totalPrice")).isEqualTo(10500L);
        assertThat(first.get("supplyPrice")).isEqualTo(8000L);
        assertThat(first.get("unitPrice")).isEqualTo(3000L);   // 6000 / 4 * 2
        assertThat(first.get("compQty")).isEqualTo(2L);        // 4 / 2
        assertThat(first.get("shipMonth")).isEqualTo("11");
        assertThat(first.get("shipDay")).isEqualTo("21");

        Map<String, Object> second = base();
        second.put("orderProdNo", 2);
        Map<String, Object> o = ShipPrintRows.print2(second);
        assertThat(o.get("salePrice")).isEqualTo(0L);
        assertThat(o.get("nvPrice")).isEqualTo(0L);
        assertThat(o.get("totalPrice")).isEqualTo(0L);
        assertThat(o.get("supplyPrice")).isEqualTo(0L);
        assertThat(o.get("subTotal")).isEqualTo(6000L);   // 소계는 그대로
    }

    @Test
    void 발주내역2는_주문수량_0이나_발주일_없음에서도_예외가_나지_않는다() {
        Map<String, Object> r = base();
        r.put("orderProdQty", 0);
        r.put("shipCheckDate", null);
        Map<String, Object> o = ShipPrintRows.print2(r);

        assertThat(o.get("compQty")).isEqualTo(4L);
        assertThat(o.get("unitPrice")).isEqualTo(0L);   // 6000 / 4 * 0
        assertThat(o.get("shipMonth")).isEqualTo("");
        assertThat(o.get("shipDay")).isEqualTo("");
    }

    // ── 엑셀 ─────────────────────────────────────────────────

    private static Workbook open(byte[] bytes) throws IOException {
        return new HSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private static Map<String, Object> printRow(String place, int orderNo, String addr, String name, int bundle) {
        Map<String, Object> r = base();
        r.put("orderNo", orderNo);
        r.put("shipPlace", place);
        r.put("recverAddr", addr);
        r.put("recverName", name);
        r.put("bundleYn", bundle);
        return ShipPrintRows.print1(r);
    }

    @Test
    void 발주내역_엑셀은_출고지마다_시트가_나뉘고_금지문자는_시트이름에서_바뀐다() throws IOException {
        List<Map<String, Object>> rows = List.of(
                printRow("보물유통", 1, "서울", "가", 0),
                printRow("보물유통", 2, "부산", "나", 0),
                printRow("택배비인상/제주", 3, "제주", "다", 0));

        try (Workbook wb = open(ShipPrintExcel.buildPrint1(rows))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("보물유통");
            assertThat(wb.getSheetName(1)).isEqualTo("택배비인상_제주");
            Sheet first = wb.getSheetAt(0);
            assertThat(first.getLastRowNum()).isEqualTo(2);
            assertThat(first.getRow(0).getCell(0).getStringCellValue()).isEqualTo("주문번호");
            assertThat(first.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1);
            // 공급단가·공급소계는 "0" 이 아니라 계산 값
            int unitCol = headerIndex(first, "공급단가");
            assertThat(first.getRow(1).getCell(unitCol).getNumericCellValue()).isEqualTo(1500);
            assertThat(first.getRow(1).getCell(unitCol + 1).getNumericCellValue()).isEqualTo(6000);
        }
    }

    @Test
    void 발주내역_엑셀은_행이_없어도_헤더만_있는_기본출고지_시트를_만든다() throws IOException {
        try (Workbook wb = open(ShipPrintExcel.buildPrint1(List.of()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetName(0)).isEqualTo("기본출고지");
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }

    @Test
    void 합배송_묶음은_배경색이_번갈아_칠해지고_합배송_열에_표시된다() throws IOException {
        List<Map<String, Object>> rows = List.of(
                printRow("보물유통", 1, "서울", "가", 1),
                printRow("보물유통", 2, "서울", "가", 1),
                printRow("보물유통", 3, "부산", "나", 1),
                printRow("보물유통", 4, "대구", "다", 0));

        try (Workbook wb = open(ShipPrintExcel.buildPrint1(rows))) {
            Sheet s = wb.getSheetAt(0);
            short a = s.getRow(1).getCell(0).getCellStyle().getFillForegroundColor();
            short a2 = s.getRow(2).getCell(0).getCellStyle().getFillForegroundColor();
            short b = s.getRow(3).getCell(0).getCellStyle().getFillForegroundColor();
            assertThat(a2).isEqualTo(a);
            assertThat(b).isNotEqualTo(a);
            assertThat(s.getRow(4).getCell(0).getCellStyle().getFillPattern().name()).isEqualTo("NO_FILL");
            int bundleCol = headerIndex(s, "합배송");
            assertThat(s.getRow(1).getCell(bundleCol).getStringCellValue()).isEqualTo("합배송");
            assertThat(s.getRow(4).getCell(bundleCol).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void 발주내역2_엑셀은_시트_하나에_26열이고_시트이름은_상점명에서_정리된다() throws IOException {
        Map<String, Object> r = base();
        r.put("prodMemo01", "A-1");
        r.put("unitName", "20L");
        List<Map<String, Object>> rows = List.of(ShipPrintRows.print2(r));

        try (Workbook wb = open(ShipPrintExcel.buildPrint2("상점[A]:가*나?", rows))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetName(0)).isEqualTo("상점_A__가_나_");
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getRow(0).getLastCellNum()).isEqualTo((short) 26);
            assertThat(s.getRow(1).getCell(headerIndex(s, "채널상품번호")).getStringCellValue()).isEqualTo("A-1");
            assertThat(s.getRow(1).getCell(headerIndex(s, "단량")).getStringCellValue()).isEqualTo("20L");
            assertThat(s.getRow(1).getCell(headerIndex(s, "판매금액")).getNumericCellValue()).isEqualTo(10500);
        }
    }

    private static int headerIndex(Sheet s, String header) {
        for (int i = 0; i < s.getRow(0).getLastCellNum(); i++) {
            if (header.equals(s.getRow(0).getCell(i).getStringCellValue())) return i;
        }
        throw new AssertionError("열 없음: " + header);
    }
}
