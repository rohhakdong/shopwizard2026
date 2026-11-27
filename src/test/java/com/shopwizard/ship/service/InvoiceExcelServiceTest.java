package com.shopwizard.ship.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.ship.mapper.SlnkInvoiceMapper;
import com.shopwizard.ship.model.InvoiceExcelResult;
import com.shopwizard.ship.model.SlnkInvoiceLine;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 송장 엑셀 일괄 입력 — 머리글·숫자 셀 읽기, 행 검사(형식·상태·택배사·범위·중복), 검사는 저장하지 않음, 등록은 정상 행만 저장하고 저장 뒤 샵링커 전송. */
class InvoiceExcelServiceTest {

    private ShipDirectService shipDirectService;
    private SlnkInvoiceService slnkInvoice;
    private SlnkInvoiceMapper mapper;
    private InvoiceExcelService service;
    private static final BiPredicate<Integer, Integer> ALL = (o, p) -> true;
    private static final AuthSession.Actor ACTOR = new AuthSession.Actor("u", "사용자");

    @BeforeEach
    void setUp() {
        shipDirectService = mock(ShipDirectService.class);
        slnkInvoice = mock(SlnkInvoiceService.class);
        mapper = mock(SlnkInvoiceMapper.class);
        service = new InvoiceExcelService(shipDirectService, slnkInvoice, mapper);
        when(slnkInvoice.mode()).thenReturn("dry-run");
        when(mapper.countDeliComp("CJ-GLS택배")).thenReturn(1);
        when(slnkInvoice.pushLine(any(), any(), any(), any())).thenReturn(new SlnkInvoiceService.Result("dry", "시험 모드"));
    }

    private static SlnkInvoiceLine lineIn(String state) {
        SlnkInvoiceLine l = new SlnkInvoiceLine();
        l.setOrderState(state);
        return l;
    }

    /** 첫 행이 머리글인 xls. 값은 문자열 또는 숫자(double). */
    private static byte[] xls(String[] headers, Object[]... rows) throws IOException {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("송장");
            Row h = s.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Object[] vals : rows) {
                Row row = s.createRow(r++);
                for (int i = 0; i < vals.length; i++) {
                    if (vals[i] instanceof Number n) row.createCell(i).setCellValue(n.doubleValue());
                    else if (vals[i] != null) row.createCell(i).setCellValue(String.valueOf(vals[i]));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static final String[] STD = {"주문번호", "주문상품번호", "주문변경번호", "택배회사명", "송장번호"};

    private InvoiceExcelResult preview(byte[] file) {
        return service.preview(new ByteArrayInputStream(file), ALL);
    }

    @Test
    void 정상_행은_OK이고_숫자_셀_송장번호는_지수_표기_없이_읽고_변경번호가_비면_1이다() throws IOException {
        when(mapper.selectLine(100, 1, 1)).thenReturn(lineIn("발주확인"));

        InvoiceExcelResult r = preview(xls(STD, new Object[]{100.0, 1.0, null, "CJ-GLS택배", 123456789012.0}));

        assertThat(r.getFileError()).isNull();
        assertThat(r.getRows()).hasSize(1);
        InvoiceExcelResult.Row row = r.getRows().get(0);
        assertThat(row.getStatus()).isEqualTo("OK");
        assertThat(row.getOrderChangeNo()).isEqualTo(1);
        assertThat(row.getInvNo()).isEqualTo("123456789012");
        assertThat(r.isImportable()).isTrue();
        assertThat(r.getSlnkMode()).isEqualTo("dry-run");
        verify(shipDirectService, never()).updateInvoice(anyMap());   // 검사는 저장하지 않는다
    }

    @Test
    void 송장번호의_공백_하이픈은_빼고_머리글_별칭을_인식한다() throws IOException {
        when(mapper.selectLine(100, 2, 3)).thenReturn(lineIn("발주확인"));
        String[] aliases = {"주문 번호", "주문순번", "변경번호", "택배사", "송장 번호"};

        InvoiceExcelResult r = preview(xls(aliases, new Object[]{"100", "2", "3", "CJ-GLS택배", "1234-5678 9012"}));

        assertThat(r.getRows().get(0).getStatus()).isEqualTo("OK");
        assertThat(r.getRows().get(0).getInvNo()).isEqualTo("123456789012");
    }

    @Test
    void 행마다_오류_사유를_알려_준다() throws IOException {
        when(mapper.selectLine(1, 1, 1)).thenReturn(null);
        when(mapper.selectLine(2, 1, 1)).thenReturn(lineIn("배송시작"));
        when(mapper.selectLine(3, 1, 1)).thenReturn(lineIn("출고지시"));
        when(mapper.selectLine(4, 1, 1)).thenReturn(lineIn("발주확인"));

        InvoiceExcelResult r = preview(xls(STD,
                new Object[]{1, 1, 1, "CJ-GLS택배", "111"},          // 줄 없음
                new Object[]{2, 1, 1, "CJ-GLS택배", "222"},          // 이미 송장
                new Object[]{3, 1, 1, "CJ-GLS택배", "333"},          // 상태 다름
                new Object[]{4, 1, 1, "없는택배", "444"},            // 택배사 없음
                new Object[]{"abc", 1, 1, "CJ-GLS택배", "555"},      // 주문번호 형식
                new Object[]{5, 1, 1, "CJ-GLS택배", "***"},          // 송장번호 형식
                new Object[]{6, 1, 1, "", "666"}));                  // 택배사 비어 있음

        List<InvoiceExcelResult.Row> rows = r.getRows();
        assertThat(rows.get(0).getErrors()).anyMatch(e -> e.contains("찾을 수 없습니다"));
        assertThat(rows.get(1).getErrors()).anyMatch(e -> e.contains("이미 송장이 등록"));
        assertThat(rows.get(2).getErrors()).anyMatch(e -> e.contains("발주확인 상태가 아닙니다") && e.contains("출고지시"));
        assertThat(rows.get(3).getErrors()).anyMatch(e -> e.contains("택배회사 관리에 없습니다"));
        assertThat(rows.get(4).getErrors()).anyMatch(e -> e.contains("주문번호는 숫자"));
        assertThat(rows.get(5).getErrors()).anyMatch(e -> e.contains("영문·숫자 30자"));
        assertThat(rows.get(6).getErrors()).anyMatch(e -> e.contains("택배회사명을 입력"));
        assertThat(r.getOkCount()).isZero();
        assertThat(r.isImportable()).isFalse();
    }

    @Test
    void 파일_안_중복과_데이터_범위_밖_줄은_오류() throws IOException {
        when(mapper.selectLine(any(), any(), any())).thenReturn(lineIn("발주확인"));
        BiPredicate<Integer, Integer> onlyOrder10 = (o, p) -> o == 10;

        InvoiceExcelResult r = service.preview(new ByteArrayInputStream(xls(STD,
                new Object[]{10, 1, 1, "CJ-GLS택배", "111"},
                new Object[]{10, 1, 1, "CJ-GLS택배", "222"},
                new Object[]{11, 1, 1, "CJ-GLS택배", "333"})), onlyOrder10);

        assertThat(r.getRows().get(0).getStatus()).isEqualTo("OK");
        assertThat(r.getRows().get(1).getErrors()).anyMatch(e -> e.contains("같은 줄"));
        assertThat(r.getRows().get(2).getErrors()).anyMatch(e -> e.contains("데이터 범위"));
    }

    @Test
    void 필수_열이_없거나_행이_없거나_300행을_넘거나_엑셀이_아니면_파일_오류() throws IOException {
        assertThat(preview(xls(new String[]{"주문번호", "송장번호"}, new Object[]{1, "2"})).getFileError()).contains("필수 열").contains("택배회사명");
        assertThat(preview(xls(STD)).getFileError()).contains("등록할 행이 없습니다");
        Object[][] many = new Object[InvoiceExcelService.MAX_ROWS + 1][];
        for (int i = 0; i < many.length; i++) many[i] = new Object[]{i + 1, 1, 1, "CJ-GLS택배", "1" + i};
        assertThat(preview(xls(STD, many)).getFileError()).contains("300행");
        assertThat(service.preview(new ByteArrayInputStream("not excel".getBytes()), ALL).getFileError()).contains("읽을 수 없습니다");
    }

    @Test
    void 등록은_정상_행만_저장하고_저장_뒤에_샵링커로_보낸다() throws IOException {
        when(mapper.selectLine(100, 1, 1)).thenReturn(lineIn("발주확인"));
        when(mapper.selectLine(101, 1, 1)).thenReturn(lineIn("배송시작"));
        when(shipDirectService.updateInvoice(anyMap())).thenReturn(1);

        InvoiceExcelResult r = service.importFile(new ByteArrayInputStream(xls(STD,
                new Object[]{100, 1, 1, "CJ-GLS택배", "123456789012"},
                new Object[]{101, 1, 1, "CJ-GLS택배", "999"})), ALL, ACTOR);

        assertThat(r.isImported()).isTrue();
        assertThat(r.getSavedCount()).isEqualTo(1);
        assertThat(r.getRows().get(0).getResultMessage()).isEqualTo("저장 완료");
        assertThat(r.getRows().get(0).getSlnkStatus()).isEqualTo("dry");
        assertThat(r.getRows().get(1).getStatus()).isEqualTo("ERROR");
        assertThat(r.getRows().get(1).getSlnkStatus()).isNull();

        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(shipDirectService, times(1)).updateInvoice(p.capture());
        assertThat(p.getValue()).containsEntry("orderNo", 100).containsEntry("orderProdNo", 1).containsEntry("orderChangeNo", 1)
                .containsEntry("deliCompName", "CJ-GLS택배").containsEntry("invNo", "123456789012")
                .containsEntry("changeId", "u").containsEntry("changeName", "사용자");
        verify(slnkInvoice, times(1)).pushLine(eq(100), eq(1), eq(1), eq(ACTOR));
    }

    @Test
    void 그_사이_상태가_바뀌어_0건이면_오류이고_전송하지_않으며_예외도_행_오류로_남는다() throws IOException {
        when(mapper.selectLine(any(), any(), any())).thenReturn(lineIn("발주확인"));
        when(shipDirectService.updateInvoice(anyMap())).thenReturn(0).thenThrow(new IllegalStateException("DB 오류"));

        InvoiceExcelResult r = service.importFile(new ByteArrayInputStream(xls(STD,
                new Object[]{1, 1, 1, "CJ-GLS택배", "111"}, new Object[]{2, 1, 1, "CJ-GLS택배", "222"})), ALL, ACTOR);

        assertThat(r.getSavedCount()).isZero();
        assertThat(r.getRows().get(0).getErrors()).anyMatch(e -> e.contains("그 사이 상태가 바뀌었습니다"));
        assertThat(r.getRows().get(1).getErrors()).anyMatch(e -> e.contains("저장 오류") && e.contains("DB 오류"));
        verify(slnkInvoice, never()).pushLine(any(), any(), any(), any());
    }

    @Test
    void 양식은_머리글_예시_안내_시트를_가진_xls다() throws IOException {
        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(service.buildTemplate()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Row h = wb.getSheetAt(0).getRow(0);
            assertThat(h.getLastCellNum()).isEqualTo((short) 5);
            assertThat(h.getCell(3).getStringCellValue()).isEqualTo("택배회사명");
            assertThat(wb.getSheetName(1)).isEqualTo("작성 안내");
        }
    }
}
