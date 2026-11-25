package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipAutoOrderMapper;
import com.shopwizard.ship.model.ShipCheck;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 발주서 엑셀(출고지별 시트·열·사은품 표시·단가 계산)과 다운로드 서비스(재출력만 출력 횟수 증가). */
class ShipOrderSheetExcelTest {

    private static Map<String, Object> row(String place, int orderNo, String prod, long qty, long buy, String addr, String name, int bundle) {
        Map<String, Object> r = new HashMap<>();
        r.put("shipPlace", place);
        r.put("orderNo", orderNo);
        r.put("orderType", "정상주문");
        r.put("chnlOrderNo", "CH-" + orderNo);
        r.put("orderReciptDate", "2026-11-20 10:11:12");
        r.put("orderName", "주문자");
        r.put("prodName", prod);
        r.put("itemName", "");
        r.put("prodQty", qty);
        r.put("salePrice", 10000);
        r.put("nvPrice", 500);
        r.put("buyPrice", buy);
        r.put("recverName", name);
        r.put("recverAddr", addr);
        r.put("recverZipcode", "12345");
        r.put("deliFeeAmt", 3000);
        r.put("deliFeeType", "선불");
        r.put("chnlName", "쿠팡");
        r.put("bundleYn", bundle);
        return r;
    }

    private static Workbook open(byte[] bytes) throws IOException {
        return new HSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    void 출고지마다_시트가_나뉘고_첫_행은_헤더다() throws IOException {
        List<Map<String, Object>> rows = List.of(
                row("본사창고", 1, "상품A", 2, 3000, "서울", "홍길동", 0),
                row("본사창고", 2, "상품B", 1, 1000, "부산", "김철수", 0),
                row("외부창고", 3, "상품C", 1, 500, "대구", "이영희", 0));

        try (Workbook wb = open(ShipOrderSheetExcel.build(rows))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("본사창고");
            assertThat(wb.getSheetName(1)).isEqualTo("외부창고");
            Sheet first = wb.getSheetAt(0);
            assertThat(first.getRow(0).getCell(0).getStringCellValue()).isEqualTo("주문번호");
            assertThat(first.getRow(0).getCell(ShipOrderSheetExcel.HEADERS.length - 1).getStringCellValue()).isEqualTo("합배송");
            assertThat(first.getLastRowNum()).isEqualTo(2);   // 헤더 + 2행
            assertThat(wb.getSheetAt(1).getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void 수량_금액_공급단가_합배송_사은품_표시를_채운다() throws IOException {
        Map<String, Object> main = row("기본출고지", 1, "상품A", 2, 3000, "서울 A", "홍길동", 1);
        Map<String, Object> gift = row("기본출고지", 1, "힣마스크", 1, 0, "서울 A", "홍길동", 1);
        gift.put("orderType", "사은품");
        gift.put("salePrice", 0);
        gift.put("nvPrice", 0);
        gift.put("deliFeeAmt", 0);

        try (Workbook wb = open(ShipOrderSheetExcel.build(List.of(main, gift)))) {
            Sheet s = wb.getSheetAt(0);
            var r1 = s.getRow(1);
            assertThat(r1.getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat(r1.getCell(1).getStringCellValue()).isEmpty();                 // 구분 비어 있음
            assertThat(r1.getCell(3).getStringCellValue()).isEqualTo("2026-11-20");   // 주문일은 날짜만
            assertThat(r1.getCell(8).getNumericCellValue()).isEqualTo(2);              // 수량
            assertThat(r1.getCell(9).getNumericCellValue()).isEqualTo(10500);          // 판매가 + NV
            assertThat(r1.getCell(17).getNumericCellValue()).isEqualTo(1500);          // 공급단가 = 3000 / 2
            assertThat(r1.getCell(18).getNumericCellValue()).isEqualTo(3000);          // 공급소계
            assertThat(r1.getCell(22).getStringCellValue()).isEqualTo("합배송");
            var r2 = s.getRow(2);
            assertThat(r2.getCell(1).getStringCellValue()).isEqualTo("사은품");
            assertThat(r2.getCell(6).getStringCellValue()).isEqualTo("힣마스크");
            assertThat(r2.getCell(9).getNumericCellValue()).isZero();
            assertThat(r2.getCell(17).getNumericCellValue()).isZero();
        }
    }

    @Test
    void 행이_없어도_헤더만_있는_기본출고지_시트를_만든다() throws IOException {
        try (Workbook wb = open(ShipOrderSheetExcel.build(List.of()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetName(0)).isEqualTo("기본출고지");
            assertThat(wb.getSheetAt(0).getLastRowNum()).isZero();
        }
    }

    @Test
    void 시트_이름은_금지_문자를_바꾸고_31자로_자르고_겹치면_번호를_붙인다() throws IOException {
        assertThat(ShipOrderSheetExcel.sheetName("A/B:C[1]?*")).isEqualTo("A_B_C_1___");
        assertThat(ShipOrderSheetExcel.sheetName("가".repeat(40))).hasSize(31);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("창고/1", 1, "A", 1, 0, "a", "n", 0));
        rows.add(row("창고:1", 2, "B", 1, 0, "b", "m", 0));   // 둘 다 "창고_1" 이 된다
        try (Workbook wb = open(ShipOrderSheetExcel.build(rows))) {
            assertThat(wb.getSheetName(0)).isEqualTo("창고_1");
            assertThat(wb.getSheetName(1)).isEqualTo("창고_1_2");
        }
    }

    // ── 다운로드 서비스 ───────────────────────────────────────────

    private final ShipAutoOrderMapper mapper = mock(ShipAutoOrderMapper.class);
    private final ShipCheckService checkService = mock(ShipCheckService.class);
    private final ShipAutoOrderService service = new ShipAutoOrderService(mapper, checkService);

    @Test
    void 지금_받는_발주서는_상태도_출력_횟수도_바꾸지_않는다() {
        when(mapper.selectShopName("S1")).thenReturn("두리두리");
        when(mapper.selectSheetRows("S1", null, null, null)).thenReturn(List.of());

        var file = service.download(" S1 ", null, "admin", "관리자");

        assertThat(file.fileName()).startsWith("발주서_두리두리_").endsWith(".xls");
        assertThat(file.bytes()).isNotEmpty();
        verify(checkService, never()).updatePrintCount(any());
    }

    @Test
    void 발주_목록_건을_다시_받으면_그_시각의_행을_뽑고_출력_횟수만_올린다() {
        ShipCheck h = new ShipCheck();
        h.setShipCheckNo(7);
        h.setShopCode("S1");
        h.setShipCheckDate("2026-11-25 14:30:00");
        h.setShipCheckDesc("발주_두리두리_2026년11월25일오후14");
        when(checkService.find(7)).thenReturn(h);
        when(mapper.selectSheetRows("S1", "2026-11-25 14:30:00", null, null)).thenReturn(List.of());

        var file = service.download(null, 7, "admin", "관리자");

        assertThat(file.fileName()).isEqualTo("발주_두리두리_2026년11월25일오후14.xls");
        verify(checkService).updatePrintCount(any(ShipCheck.class));
    }

    @Test
    void 없는_발주_목록이나_빈_상점은_거절한다() {
        assertThatThrownBy(() -> service.download(null, 99, "a", "b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.download("  ", null, "a", "b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.summary(null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.shopOfCheck(99)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 파일명의_금지_문자는_밑줄로_바꾼다() {
        assertThat(ShipAutoOrderService.fileName("A/B:C", LocalDateTime.of(2026, 11, 25, 14, 5)))
                .isEqualTo("발주서_A_B_C_20261125_1405.xls");
        assertThat(ShipAutoOrderService.fileName(null, LocalDateTime.of(2026, 11, 25, 14, 5)))
                .isEqualTo("발주서_발주서_20261125_1405.xls");
    }

    @Test
    void 쪽_나눔은_상한을_넘지_않고_음수_시작은_0이다() {
        when(mapper.selectSheetCount("S1", null)).thenReturn(1234);
        when(mapper.selectSheetRows(eq("S1"), eq(null), eq(0), eq(500))).thenReturn(List.of());

        var out = service.sheet("S1", -5, 100000);

        assertThat(out).containsEntry("total", 1234);
        verify(mapper).selectSheetRows("S1", null, 0, 500);
    }
}
