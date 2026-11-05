package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdExcelMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdExcelResult;
import com.shopwizard.catalog.model.ProdExcelRow;
import com.shopwizard.code.model.ConstrVal;
import com.shopwizard.code.service.ConstrValService;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProdExcelServiceTest {

    private static final String[] ALL_HEADERS = Arrays.stream(ProdExcelService.COLUMNS).map(c -> c[0]).toArray(String[]::new);

    private final ProdExcelMapper mapper = mock(ProdExcelMapper.class);
    private final ProdService prodService = mock(ProdService.class);
    private final ConstrValService constrValService = mock(ConstrValService.class);
    private final DataScopeGuard scopeGuard = mock(DataScopeGuard.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private ProdExcelService service;

    @BeforeEach
    void setUp() {
        service = new ProdExcelService(mapper, prodService, constrValService, scopeGuard, txManager);
        when(scopeGuard.scope(any())).thenReturn(DataScope.UNRESTRICTED);
        when(mapper.selectShopSvc("SHOP00000001")).thenReturn("SVC1");
        when(mapper.countLeafCate("SVC1", "G006010009001")).thenReturn(1);
        when(mapper.countBrand(any(), anyString())).thenReturn(1);
        when(mapper.countMaker(anyString())).thenReturn(1);
        when(mapper.countOrigin(anyString())).thenReturn(1);
        when(mapper.selectMaxProdCode()).thenReturn("1000000342");
        when(constrValService.findConstrValList(any())).thenAnswer(inv -> {
            Map<String, Object> q = inv.getArgument(0);
            return switch (String.valueOf(q.get("pConstrCode"))) {
                case "cTaxType" -> List.of(cv("10", "과세"), cv("0", "면세"));
                case "cDeliFeeType" -> List.of(cv("무료", "무료"), cv("선불", "선불"), cv("착불", "착불"));
                case "cSaleYnType" -> List.of(cv("-1", "승인대기"), cv("0", "일시중지"), cv("1", "정상판매"), cv("-9", "완전품절"));
                default -> List.of();
            };
        });
    }

    private static ConstrVal cv(String val, String desc) {
        ConstrVal c = new ConstrVal();
        c.setConstrVal(val);
        c.setConstrValDesc(desc);
        return c;
    }

    // ── 엑셀 파일 만들기 ──────────────────────────────────────

    private static byte[] book(boolean xlsx, String[] headers, List<Object[]> rows) throws Exception {
        try (Workbook wb = xlsx ? new XSSFWorkbook() : new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("Sheet1");
            Row h = s.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Object[] row : rows) {
                Row x = s.createRow(r++);
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null) continue;
                    if (row[i] instanceof Number n) x.createCell(i).setCellValue(n.doubleValue());
                    else x.createCell(i).setCellValue(String.valueOf(row[i]));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 양식 35열 순서로 값을 채운 한 행 — 이름/값 쌍으로 쓰고 나머지는 빈 칸. */
    private static Object[] row(Object... kv) {
        Object[] r = new Object[ALL_HEADERS.length];
        for (int i = 0; i < kv.length; i += 2) {
            int idx = Arrays.asList(ALL_HEADERS).indexOf(kv[i]);
            assertTrue(idx >= 0, "알 수 없는 열: " + kv[i]);
            r[idx] = kv[i + 1];
        }
        return r;
    }

    private static InputStream xls(Object[]... rows) throws Exception {
        return new ByteArrayInputStream(book(false, ALL_HEADERS, List.of(rows)));
    }

    private static Object[] goodNew() {
        return row("상품명칭", "루벨리 V라인", "상점코드", "SHOP00000001", "분류코드", "G006010009001");
    }

    // ── 양식 / 읽기 ───────────────────────────────────────────

    @Test
    void 양식은_예전_양식과_같은_35열_순서이고_안내_시트가_있다() throws Exception {
        assertEquals(35, ALL_HEADERS.length);
        assertEquals("상품코드", ALL_HEADERS[0]);
        assertEquals("상품명칭", ALL_HEADERS[2]);
        assertEquals("변경 일자", ALL_HEADERS[34]);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(service.buildTemplate()))) {
            assertEquals("Sheet1", wb.getSheetAt(0).getSheetName());
            assertEquals("작성안내", wb.getSheetAt(1).getSheetName());
            Row h = wb.getSheetAt(0).getRow(0);
            assertEquals(35, h.getLastCellNum());
            for (int i = 0; i < 35; i++) assertEquals(ALL_HEADERS[i], h.getCell(i).getStringCellValue());
        }
    }

    @Test
    void 빈_양식을_올리면_데이터_행이_없다고_알린다() throws Exception {
        ProdExcelResult r = service.preview(new ByteArrayInputStream(service.buildTemplate()), request);
        assertNotNull(r.getFileError());
        assertTrue(r.getFileError().contains("상품 행이 없습니다"));
        assertFalse(r.isImportable());
    }

    @Test
    void 엑셀이_아닌_파일은_읽을_수_없다고_알린다() {
        ProdExcelResult r = service.preview(new ByteArrayInputStream("hello".getBytes()), request);
        assertTrue(r.getFileError().contains("읽을 수 없습니다"));
    }

    @Test
    void xlsx도_읽는다() throws Exception {
        byte[] b = book(true, ALL_HEADERS, List.<Object[]>of(goodNew()));
        ProdExcelResult r = service.preview(new ByteArrayInputStream(b), request);
        assertNull(r.getFileError());
        assertEquals(1, r.getRows().size());
        assertTrue(r.isImportable());
    }

    @Test
    void 열은_순서가_아니라_헤더_이름으로_찾고_헤더_공백은_무시한다() throws Exception {
        String[] headers = {"상점코드", "분류 코드", "상품명칭"};   // 순서 바뀜, '분류 코드' 공백
        byte[] b = book(false, headers, List.<Object[]>of(new Object[]{"SHOP00000001", "G006010009001", "순서 바뀐 상품"}));
        ProdExcelResult r = service.preview(new ByteArrayInputStream(b), request);
        assertTrue(r.isImportable(), String.valueOf(r.getRows().get(0).getErrors()));
        assertEquals("순서 바뀐 상품", r.getRows().get(0).getProd().getProdName());
        assertEquals("G006010009001", r.getRows().get(0).getProd().getCateCode());
    }

    @Test
    void 상품명칭_헤더가_없으면_파일_오류() throws Exception {
        byte[] b = book(false, new String[]{"상점코드", "분류코드"}, List.<Object[]>of(new Object[]{"SHOP00000001", "G006010009001"}));
        ProdExcelResult r = service.preview(new ByteArrayInputStream(b), request);
        assertTrue(r.getFileError().contains("상품명칭"));
    }

    @Test
    void 숫자_셀은_소수점과_지수없이_평문으로_읽는다() throws Exception {
        List<ProdExcelService.RawRow> rows = service.parse(xls(
                row("상품코드", 1000000871d, "상품명칭", "x", "판매가격", 44800d, "배송비", 2500.5d)));
        ProdExcelService.RawRow r = rows.get(0);
        assertEquals("1000000871", r.get("상품코드"));
        assertEquals("44800", r.get("판매가격"));
        assertEquals("2500.5", r.get("배송비"));
        assertEquals(2, r.rowNo);   // 헤더가 1행이므로 첫 데이터 행은 2
    }

    @Test
    void 완전히_빈_행은_건너뛰고_행_번호는_엑셀_기준() throws Exception {
        InputStream in = xls(goodNew(), new Object[ALL_HEADERS.length], goodNew());
        List<ProdExcelService.RawRow> rows = service.parse(in);
        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).rowNo);
        assertEquals(4, rows.get(1).rowNo);
    }

    @Test
    void 행이_1000을_넘으면_파일_오류() throws Exception {
        List<Object[]> many = new ArrayList<>();
        for (int i = 0; i < 1001; i++) many.add(goodNew());
        ProdExcelResult r = service.preview(new ByteArrayInputStream(book(false, ALL_HEADERS, many)), request);
        assertTrue(r.getFileError().contains("1000"));
    }

    // ── 신규 검사 ─────────────────────────────────────────────

    @Test
    void 신규_정상_행은_기본값이_채워지고_DB는_바뀌지_않는다() throws Exception {
        ProdExcelResult r = service.preview(xls(goodNew()), request);

        assertTrue(r.isImportable());
        assertEquals(1, r.getNewCount());
        ProdExcelRow row = r.getRows().get(0);
        assertEquals(ProdExcelRow.NEW, row.getAction());
        assertEquals("OK", row.getStatus());
        Prod p = row.getProd();
        assertEquals("루벨리 V라인", p.getProdDsplName());   // 비우면 상품명칭
        assertEquals(-1, p.getSaleYn());                     // 비우면 승인대기
        assertEquals(10, p.getVatRate());                    // 비우면 과세
        assertEquals("무료", p.getDeliFeeType());
        assertEquals(0, p.getSalePrice());
        assertEquals(0, p.getSupplyQty());
        assertEquals(1, p.getState());
        verifyNoInteractions(prodService);
    }

    @Test
    void 한글_코드값을_코드로_바꾼다() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001",
                "부가세율", "면세", "배송비구분", "착불", "판매상태", "정상판매", "판매가격", "44,800", "재고수량", 999999d)), request);

        Prod p = r.getRows().get(0).getProd();
        assertEquals(0, p.getVatRate());
        assertEquals("착불", p.getDeliFeeType());
        assertEquals(1, p.getSaleYn());
        assertEquals(44800, p.getSalePrice());   // 콤마 제거
        assertEquals(999999, p.getSupplyQty());
    }

    @Test
    void 코드값이_틀리면_사용_가능한_값을_알려준다() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001",
                "부가세율", "영세", "판매상태", "품절")), request);

        List<String> errors = r.getRows().get(0).getErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("과세 / 면세"));
        assertTrue(errors.get(1).contains("정상판매"));
        assertFalse(r.isImportable());
    }

    @Test
    void 이미지는_모든_크기에_같은_경로를_넣고_이상한_경로는_경고() throws Exception {
        ProdExcelResult r = service.preview(xls(
                row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001", "이미지", "/images/product/a.jpg"),
                row("상품명칭", "b", "상점코드", "SHOP00000001", "분류코드", "G006010009001", "이미지", "C:\\pic\\b.jpg")), request);

        Prod a = r.getRows().get(0).getProd();
        assertEquals("/images/product/a.jpg", a.getImgUrl());
        assertEquals("/images/product/a.jpg", a.getImgUrl50());
        assertEquals("/images/product/a.jpg", a.getImgUrl280());
        assertTrue(r.getRows().get(0).getWarnings().isEmpty());
        assertEquals(1, r.getRows().get(1).getWarnings().size());
        assertTrue(r.isImportable());   // 경고는 등록을 막지 않는다
    }

    @Test
    void 필수값_누락은_행마다_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "")), request);   // 전부 빈 행은 건너뛰므로 상품전시명칭만 채움
        // 모두 빈 행이면 건너뛰어 '행 없음' 파일 오류 — 필수 누락을 보려면 다른 칸이 하나는 있어야 한다.
        assertNotNull(r.getFileError());

        r = service.preview(xls(row("판매가격", 1000d)), request);
        List<String> errors = r.getRows().get(0).getErrors();
        assertTrue(errors.contains("상품명칭은 필수입니다."));
        assertTrue(errors.contains("상점코드는 필수입니다."));
        assertTrue(errors.contains("분류코드는 필수입니다."));
    }

    @Test
    void 존재하지_않는_상점과_분류는_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(
                row("상품명칭", "a", "상점코드", "NOPE00000000", "분류코드", "G006010009001"),
                row("상품명칭", "b", "상점코드", "SHOP00000001", "분류코드", "G999999999999"),
                row("상품명칭", "c", "상점코드", "SHOP00000001", "분류코드", "짧음")), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("존재하지 않는 상점코드"));
        assertTrue(r.getRows().get(1).getErrors().get(0).contains("최종 분류가 아닙니다"));
        assertTrue(r.getRows().get(2).getErrors().get(0).contains("13자리"));
        assertEquals(3, r.getErrorCount());
    }

    @Test
    void 상점상품코드는_DB와_파일_안에서_중복되면_오류() throws Exception {
        when(mapper.countShopProdCode("DUP-DB", null)).thenReturn(1);

        ProdExcelResult r = service.preview(xls(
                row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001", "상점상품코드", "DUP-DB"),
                row("상품명칭", "b", "상점코드", "SHOP00000001", "분류코드", "G006010009001", "상점상품코드", "SAME"),
                row("상품명칭", "c", "상점코드", "SHOP00000001", "분류코드", "G006010009001", "상점상품코드", "SAME")), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("이미 다른 상품이 사용 중"));
        assertTrue(r.getRows().get(1).getErrors().isEmpty());
        assertTrue(r.getRows().get(2).getErrors().get(0).contains("파일 안에서 중복"));
    }

    @Test
    void 숫자_열의_잘못된_값은_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001",
                "판매가격", "abc", "기존가격", -1d, "공급가격", 10.5d, "재고수량", 99999999999d)), request);

        List<String> e = r.getRows().get(0).getErrors();
        assertEquals(4, e.size(), e.toString());
    }

    @Test
    void 길이를_넘으면_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "a".repeat(256), "상점코드", "SHOP00000001",
                "분류코드", "G006010009001", "브랜드", "b".repeat(51))), request);

        List<String> e = r.getRows().get(0).getErrors();
        assertEquals(2, e.size(), e.toString());
        assertTrue(e.get(0).contains("255자"));
    }

    @Test
    void 마스터에_없는_브랜드_제조사_원산지는_경고만_하고_그대로_저장한다() throws Exception {
        when(mapper.countBrand(any(), eq("루벨리"))).thenReturn(0);
        when(mapper.countMaker("루벨리")).thenReturn(0);
        when(mapper.countOrigin("한국")).thenReturn(0);

        ProdExcelResult r = service.preview(xls(row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001",
                "브랜드", "루벨리", "제조사", "루벨리", "원산지", "한국")), request);

        ProdExcelRow row = r.getRows().get(0);
        assertEquals(3, row.getWarnings().size());
        assertEquals("OK", row.getStatus());
        assertEquals("루벨리", row.getProd().getBrandName());
        assertEquals("한국", row.getProd().getOriginName());
        assertTrue(r.isImportable());
        assertEquals(1, r.getWarningCount());
    }

    @Test
    void 상품설명이_65535바이트를_넘으면_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품명칭", "a", "상점코드", "SHOP00000001", "분류코드", "G006010009001",
                "상품설명", "가".repeat(22_000))), request);   // 한글 3바이트 × 22,000 = 66,000
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("너무 깁니다"));
    }

    @Test
    void 상점_제한_계정은_다른_상점_행이_오류() throws Exception {
        when(scopeGuard.scope(any())).thenReturn(new DataScope("OTHERSHOP000", null, null));

        ProdExcelResult r = service.preview(xls(goodNew()), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("권한이 없는 상점"));
    }

    @Test
    void 서비스_제한_계정은_다른_서비스_상점이_오류() throws Exception {
        when(scopeGuard.scope(any())).thenReturn(new DataScope(null, null, "OTHERSVC"));

        ProdExcelResult r = service.preview(xls(goodNew()), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("다른 서비스의 상점"));
    }

    // ── 수정 검사 ─────────────────────────────────────────────

    private Prod existing(String code) {
        Prod p = new Prod();
        p.setProdCode(code);
        p.setProdName("기존 상품명");
        p.setShopCode("SHOP00000001");
        p.setCateCode("G006010009001");
        p.setSalePrice(5000);
        p.setSupplyQty(77);
        p.setBrandName("기존브랜드");
        return p;
    }

    @Test
    void 상품코드가_있으면_수정이고_빈_칸은_기존_값을_유지한다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));

        ProdExcelResult r = service.preview(xls(row("상품코드", 1000000100d, "상품명칭", "새 이름", "판매가격", 6000d)), request);

        ProdExcelRow row = r.getRows().get(0);
        assertEquals(ProdExcelRow.UPDATE, row.getAction());
        assertEquals("OK", row.getStatus(), row.getErrors().toString());
        Prod p = row.getProd();
        assertEquals("새 이름", p.getProdName());
        assertEquals(6000, p.getSalePrice());
        assertEquals(77, p.getSupplyQty());              // 빈 칸 → 유지
        assertEquals("기존브랜드", p.getBrandName());      // 빈 칸 → 유지
        assertEquals("SHOP00000001", p.getShopCode());
        assertEquals(1, r.getUpdateCount());
    }

    @Test
    void 수정은_상품명칭이_비어도_된다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000100", "판매가격", 6000d)), request);

        assertEquals("OK", r.getRows().get(0).getStatus());
        assertEquals("기존 상품명", r.getRows().get(0).getProdName());
    }

    @Test
    void 존재하지_않는_상품코드는_오류_신규면_비우라고_안내() throws Exception {
        when(mapper.countProd("1000000871")).thenReturn(0);

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000871", "상품명칭", "x")), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("존재하지 않는 상품코드"));
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("비워"));
    }

    @Test
    void 상품코드_형식이_틀리면_오류() throws Exception {
        ProdExcelResult r = service.preview(xls(row("상품코드", "ABC", "상품명칭", "x")), request);
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("10자리 숫자"));
    }

    @Test
    void 권한이_없는_상품이면_오류() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(scopeGuard).requireCatProd(any(), eq("1000000100"));

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000100", "상품명칭", "x")), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("권한이 없는 상품"));
        verify(prodService, never()).select(anyString());
    }

    @Test
    void 같은_상품코드가_두_번이면_오류() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));

        ProdExcelResult r = service.preview(xls(
                row("상품코드", "1000000100", "상품명칭", "a"),
                row("상품코드", "1000000100", "상품명칭", "b")), request);

        assertTrue(r.getRows().get(1).getErrors().stream().anyMatch(m -> m.contains("두 번 이상")));
    }

    @Test
    void 게시된_상품을_수정하면_판매_상품에_바로_반영된다고_경고한다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(mapper.countPublished("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000100", "판매가격", 1d)), request);

        assertEquals(1, r.getRows().get(0).getWarnings().size());
        assertTrue(r.getRows().get(0).getWarnings().get(0).contains("판매 상품에 바로 반영"));
        assertTrue(r.isImportable());
    }

    @Test
    void 수정에서_상점상품코드는_자기_자신과_겹쳐도_된다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));
        when(mapper.countShopProdCode("SKU-1", "1000000100")).thenReturn(0);

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000100", "상점상품코드", "SKU-1")), request);

        assertEquals("OK", r.getRows().get(0).getStatus());
        assertEquals("SKU-1", r.getRows().get(0).getProd().getShopProdCode());
    }

    @Test
    void 수정에서_상점을_바꾸면_분류를_새_서비스_기준으로_다시_검사한다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));
        when(mapper.selectShopSvc("SHOP00000002")).thenReturn("SVC2");
        when(mapper.countLeafCate("SVC2", "G006010009001")).thenReturn(0);

        ProdExcelResult r = service.preview(xls(row("상품코드", "1000000100", "상점코드", "SHOP00000002")), request);

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("최종 분류가 아닙니다"));
    }

    // ── 등록 ──────────────────────────────────────────────────

    @Test
    void 오류가_하나라도_있으면_아무것도_저장하지_않는다() throws Exception {
        ProdExcelResult r = service.importAll(xls(goodNew(), row("상품명칭", "상점 없음")), request);

        assertFalse(r.isImported());
        assertEquals(1, r.getErrorCount());
        verify(prodService, never()).insert(any());
        verify(prodService, never()).update(any());
    }

    @Test
    void 정상이면_신규는_최댓값_다음부터_순서대로_채번해_저장하고_수정은_update를_부른다() throws Exception {
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenReturn(existing("1000000100"));
        AuthSession.Actor actor = new AuthSession.Actor("admin1", "관리자");
        when(request.getAttribute(AuthSession.ACTOR_ATTR)).thenReturn(actor);

        ProdExcelResult r = service.importAll(xls(
                goodNew(),
                row("상품코드", "1000000100", "판매가격", 7000d),
                row("상품명칭", "둘째", "상점코드", "SHOP00000001", "분류코드", "G006010009001")), request);

        assertTrue(r.isImported());
        assertEquals(2, r.getNewCount());
        assertEquals(1, r.getUpdateCount());
        assertEquals("1000000343", r.getRows().get(0).getProdCode());
        assertEquals("1000000100", r.getRows().get(1).getProdCode());
        assertEquals("1000000344", r.getRows().get(2).getProdCode());

        ArgumentCaptor<Prod> ins = ArgumentCaptor.forClass(Prod.class);
        verify(prodService, times(2)).insert(ins.capture());
        assertEquals("1000000343", ins.getAllValues().get(0).getProdCode());
        assertEquals("admin1", ins.getAllValues().get(0).getRegistId());
        assertEquals("관리자", ins.getAllValues().get(0).getRegistName());
        assertEquals("admin1", ins.getAllValues().get(0).getChangeId());

        ArgumentCaptor<Prod> upd = ArgumentCaptor.forClass(Prod.class);
        verify(prodService).update(upd.capture());
        assertEquals(7000, upd.getValue().getSalePrice());
        assertEquals("admin1", upd.getValue().getChangeId());
        assertNull(upd.getValue().getRegistId());   // 수정은 등록자를 바꾸지 않는다
    }

    // ── 내려받기(export) ─────────────────────────────────────

    /** 내려받기 → 다시 올리기 왕복을 시험하려고 일부러 까다로운 값(최종 분류가 아닌 분류, 코드표에 없는 값, 썸네일)을 가진 상품. */
    private static Prod fullProd() {
        Prod p = new Prod();
        p.setProdCode("1000000100"); p.setShopProdCode("SKU-1");
        p.setProdName("상품"); p.setProdDsplName("전시명");
        p.setCateCode("G006010009001"); p.setCateName("링클/탄력케어");
        p.setBrandName("브랜드"); p.setMakerName("제조사"); p.setOriginName("한국");
        p.setShopCode("SHOP00000001"); p.setShopName("상점");
        p.setListPrice(100); p.setSalePrice(5000); p.setSupplyPrice(50); p.setBuyPrice(40);
        p.setVatRate(10); p.setSaleYn(1); p.setSupplyQty(33);
        p.setAttrName1("사이즈"); p.setAttrValStr1("L");
        p.setImgUrl("/images/p/a.jpg"); p.setImgUrl50("/images/p/a_thumbnail_50.jpg"); p.setImgUrl160("/images/p/a_tn_160.jpg");
        p.setDeliFeeType("이상한배송"); p.setDeliFeeAmt(2500);
        p.setProdDesc("<p>설명</p>"); p.setMemo01("메모"); p.setChangeDate("2026-11-05 10:00:00.0");
        return p;
    }

    private static Sheet firstSheet(byte[] xls) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(xls)).getSheetAt(0);
    }

    @Test
    void 내려받기는_같은_양식의_헤더와_값을_쓰고_코드값은_한글_이름으로_쓴다() throws Exception {
        when(prodService.selectList(any())).thenReturn(List.of(fullProd()));

        Sheet s = firstSheet(service.export(Map.of()));

        for (int i = 0; i < 35; i++) assertEquals(ALL_HEADERS[i], s.getRow(0).getCell(i).getStringCellValue());
        Row r = s.getRow(1);
        List<String> h = Arrays.asList(ALL_HEADERS);
        assertEquals("1000000100", r.getCell(h.indexOf("상품코드")).getStringCellValue());   // 문자열(지수 표기 방지)
        assertEquals(5000d, r.getCell(h.indexOf("판매가격")).getNumericCellValue());          // 숫자
        assertEquals("과세", r.getCell(h.indexOf("부가세율")).getStringCellValue());
        assertEquals("정상판매", r.getCell(h.indexOf("판매상태")).getStringCellValue());
        assertEquals("링클/탄력케어", r.getCell(h.indexOf("분류명칭")).getStringCellValue());
        assertEquals("<p>설명</p>", r.getCell(h.indexOf("상품설명")).getStringCellValue());
        assertEquals("수정", r.getCell(h.indexOf("수정")).getStringCellValue());
        assertNull(r.getCell(h.indexOf("속성명칭2")));   // 빈 값은 셀을 만들지 않는다
    }

    @Test
    void 내려받기는_화면_검색조건만_받고_정렬_같은_파라미터는_버린다() throws Exception {
        when(prodService.selectList(any())).thenReturn(List.of());
        Map<String, String> in = new HashMap<>();
        in.put("pProdName", " 루벨리 ");
        in.put("pShopCode", "SHOP00000001");
        in.put("pSaleYn", "");
        in.put("sidx", "ProdCode; DROP TABLE x");   // SQL 에 그대로 들어가는 정렬 파라미터
        in.put("sord", "DESC");
        in.put("pPageSize", "1");

        service.export(in);

        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> q = ArgumentCaptor.forClass(Map.class);
        verify(prodService).selectList(q.capture());
        Map<String, Object> sent = q.getValue();
        assertEquals("루벨리", sent.get("pProdName"));
        assertEquals("SHOP00000001", sent.get("pShopCode"));
        assertFalse(sent.containsKey("pSaleYn"));       // 빈 조건은 보내지 않는다
        assertFalse(sent.containsKey("sidx"));
        assertFalse(sent.containsKey("sord"));
        assertEquals(ProdExcelService.MAX_EXPORT + 1, sent.get("pPageSize"));   // 한도 초과를 알아내려고 1건 더
        assertEquals(0, sent.get("pPageOffset"));
    }

    @Test
    void 내려받기가_한도를_넘으면_조건을_좁히라고_거절한다() {
        List<Prod> many = new ArrayList<>();
        for (int i = 0; i <= ProdExcelService.MAX_EXPORT; i++) many.add(fullProd());
        when(prodService.selectList(any())).thenReturn(many);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.export(Map.of()));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertTrue(e.getReason().contains("좁혀"));
    }

    @Test
    void 셀_한도를_넘는_상품설명은_비우고_작성안내에_알린다() throws Exception {
        Prod p = fullProd();
        p.setProdDesc("a".repeat(ProdExcelService.MAX_CELL_CHARS + 1));
        when(prodService.selectList(any())).thenReturn(List.of(p, fullProd()));

        byte[] xls = service.export(Map.of());

        Sheet s = firstSheet(xls);
        int descCol = Arrays.asList(ALL_HEADERS).indexOf("상품설명");
        assertNull(s.getRow(1).getCell(descCol));                       // 비워 둔다(올릴 때 빈 칸 = 기존 값 유지)
        assertEquals("<p>설명</p>", s.getRow(2).getCell(descCol).getStringCellValue());
        Sheet guide = WorkbookFactory.create(new ByteArrayInputStream(xls)).getSheetAt(1);
        StringBuilder all = new StringBuilder();
        guide.forEach(row -> row.forEach(c -> all.append(c.getStringCellValue()).append('\n')));
        assertTrue(all.toString().contains("1000000100"));
        assertTrue(all.toString().contains("한도"));
    }

    @Test
    void 내려받은_파일을_그대로_다시_올려도_오류_없이_값이_그대로_유지된다() throws Exception {
        when(prodService.selectList(any())).thenReturn(List.of(fullProd()));
        byte[] xls = service.export(Map.of());

        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenAnswer(inv -> fullProd());
        // 기존 분류는 최종 분류가 아니고(countLeafCate 기본 0), 마스터에도 없고, 배송비구분은 코드표에 없는 값이다.
        when(mapper.countBrand(any(), anyString())).thenReturn(0);
        when(mapper.countMaker(anyString())).thenReturn(0);
        when(mapper.countOrigin(anyString())).thenReturn(0);

        ProdExcelResult r = service.preview(new ByteArrayInputStream(xls), request);

        ProdExcelRow row = r.getRows().get(0);
        assertEquals("OK", row.getStatus(), row.getErrors().toString());
        assertEquals(ProdExcelRow.UPDATE, row.getAction());
        assertTrue(row.getWarnings().isEmpty(), row.getWarnings().toString());   // 바꾸지 않은 값은 경고도 없다
        Prod p = row.getProd();
        assertEquals("/images/p/a_tn_160.jpg", p.getImgUrl160());                // 썸네일이 같은 경로로 덮이지 않는다
        assertEquals("/images/p/a_thumbnail_50.jpg", p.getImgUrl50());
        assertEquals("G006010009001", p.getCateCode());
        assertEquals("이상한배송", p.getDeliFeeType());
        assertEquals(10, p.getVatRate());
        assertEquals(1, p.getSaleYn());
        assertEquals(5000, p.getSalePrice());
        assertEquals(33, p.getSupplyQty());
        assertEquals("<p>설명</p>", p.getProdDesc());
        verify(mapper, never()).countLeafCate(any(), any());
    }

    @Test
    void 내려받은_파일에서_이미지와_분류를_바꾸면_그_칸만_검사하고_반영한다() throws Exception {
        when(prodService.selectList(any())).thenReturn(List.of(fullProd()));
        byte[] xls = service.export(Map.of());
        // 같은 파일의 이미지/분류 칸만 다른 값으로 바꾼다
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xls)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row r = wb.getSheetAt(0).getRow(1);
            List<String> h = Arrays.asList(ALL_HEADERS);
            r.getCell(h.indexOf("이미지")).setCellValue("/images/p/new.jpg");
            r.getCell(h.indexOf("분류코드")).setCellValue("G999999999999");
            wb.write(out);
            xls = out.toByteArray();
        }
        when(mapper.countProd("1000000100")).thenReturn(1);
        when(prodService.select("1000000100")).thenAnswer(inv -> fullProd());

        ProdExcelResult r = service.preview(new ByteArrayInputStream(xls), request);

        // 새 분류는 그 서비스의 최종 분류가 아니므로 오류, 이미지는 새 경로로 모든 크기에 들어간다
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("최종 분류가 아닙니다"));
        verify(mapper).countLeafCate("SVC1", "G999999999999");
    }

    @Test
    void 상품이_하나도_없는_DB에서는_첫_코드부터_채번한다() throws Exception {
        when(mapper.selectMaxProdCode()).thenReturn(null);

        ProdExcelResult r = service.importAll(xls(goodNew()), request);

        assertEquals("1000000001", r.getRows().get(0).getProdCode());
    }
}
