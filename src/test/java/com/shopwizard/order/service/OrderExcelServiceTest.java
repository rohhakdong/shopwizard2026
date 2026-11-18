package com.shopwizard.order.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScope;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.mapper.OrderExcelMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderExcelResult;
import com.shopwizard.order.model.OrderExcelRow;
import com.shopwizard.order.model.OrderExcelUpload;
import com.shopwizard.order.model.OrderExcelUploadDetail;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.ProdCodeMatch;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 주문 엑셀 업로드 — 양식·검증·중복·상품 매칭·정상 행만 저장·이력. 메모리에서 만든 .xls/.xlsx 로 시험한다. */
class OrderExcelServiceTest {

    private static final String[] ALL_HEADERS = Arrays.stream(OrderExcelService.COLUMNS).map(c -> c[0]).toArray(String[]::new);

    private final OrderExcelMapper mapper = mock(OrderExcelMapper.class);
    private final OrderNoSeqMapper orderNoSeqMapper = mock(OrderNoSeqMapper.class);
    private final OrderService orderService = mock(OrderService.class);
    private final OrderProdService orderProdService = mock(OrderProdService.class);
    private final OrderPayService orderPayService = mock(OrderPayService.class);
    private final StockLedgerService stockLedgerService = mock(StockLedgerService.class);
    private final ProdService prodService = mock(ProdService.class);
    private final ProdCodeMatchMapper prodCodeMatchMapper = mock(ProdCodeMatchMapper.class);
    private final DataScopeGuard scopeGuard = mock(DataScopeGuard.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AtomicInteger orderSeq = new AtomicInteger(800000);
    private OrderExcelService service;

    @BeforeEach
    void setUp() {
        service = new OrderExcelService(mapper, orderNoSeqMapper, orderService, orderProdService, orderPayService,
                stockLedgerService, prodService, prodCodeMatchMapper, scopeGuard, txManager);
        when(scopeGuard.scope(any())).thenReturn(DataScope.UNRESTRICTED);
        when(mapper.selectChnlCodesByName(eq("홈앤쇼핑"), any())).thenReturn(List.of("CH1"));
        when(mapper.selectExistingExcelOrderNos(any())).thenReturn(List.of());
        doAnswer(inv -> {
            ((OrderNoSeq) inv.getArgument(0)).setSeq(orderSeq.incrementAndGet());
            return null;
        }).when(orderNoSeqMapper).insert(any());
        doAnswer(inv -> {
            ((OrderExcelUpload) inv.getArgument(0)).setUploadId(555);
            return 1;
        }).when(mapper).insertUpload(any());
        when(stockLedgerService.claimDeduction(any(), any(), any(), any(), any())).thenReturn(true);
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
                    else if (row[i] instanceof Date d) {
                        Cell c = x.createCell(i);
                        c.setCellValue(d);
                        CellStyle st = wb.createCellStyle();
                        st.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
                        c.setCellStyle(st);
                    } else x.createCell(i).setCellValue(String.valueOf(row[i]));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 양식 28열 순서로 값을 채운 한 행 — 이름/값 쌍으로 쓰고 나머지는 빈 칸. */
    private static Object[] row(Object... kv) {
        Object[] r = new Object[ALL_HEADERS.length];
        for (int i = 0; i < kv.length; i += 2) {
            int idx = Arrays.asList(ALL_HEADERS).indexOf(kv[i]);
            assertTrue(idx >= 0, "알 수 없는 열: " + kv[i]);
            r[idx] = kv[i + 1];
        }
        return r;
    }

    private static Object[] good(Object... overrides) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("주문번호", "T001"); m.put("채널명", "홈앤쇼핑"); m.put("채널주문번호", "CO-1");
        m.put("주문자명", "홍길동"); m.put("주문자 휴대번호", "010-1111-2222");
        m.put("수취인", "김철수"); m.put("수취인 휴대번호", "010-3333-4444"); m.put("수취인 우편번호", "04524");
        m.put("수취인 주소", "서울 중구 세종대로 110"); m.put("배송 메모", "문 앞");
        m.put("상점 상품 코드", "SP-1"); m.put("상품 명칭", "상품1"); m.put("선택 옵션", "옵션A");
        m.put("공급 가격", 900); m.put("판매 가격", 1000); m.put("매입 가격", 700); m.put("주문 수량", 2);
        m.put("주문 일자", LocalDate.now().minusDays(1).toString()); m.put("배송비 타입", "무료");
        for (int i = 0; i < overrides.length; i += 2) m.put((String) overrides[i], overrides[i + 1]);
        Object[] kv = new Object[m.size() * 2];
        int i = 0;
        for (Map.Entry<String, Object> e : m.entrySet()) { kv[i++] = e.getKey(); kv[i++] = e.getValue(); }
        return row(kv);
    }

    private static InputStream xls(Object[]... rows) throws Exception {
        return new ByteArrayInputStream(book(false, ALL_HEADERS, List.of(rows)));
    }

    private static Prod prod(String code) {
        Prod p = new Prod();
        p.setProdCode(code);
        p.setShopCode("SHOP1");
        p.setShopName("상점1");
        p.setSupplyPrice(800);
        p.setBuyPrice(600);
        p.setVatRate(10);
        return p;
    }

    private void givenSlugProd(String shopProdCode, Prod p) {
        when(prodService.selectByShopProdCode(Map.of("pShopProdCode", shopProdCode))).thenReturn(p);
    }

    private OrderExcelResult check(Object[]... rows) throws Exception {
        return service.preview(xls(rows), request);
    }

    private OrderExcelResult run(Object[]... rows) throws Exception {
        return service.importFile(xls(rows), "orders.xls", request);
    }

    // ── 양식 ────────────────────────────────────────────────

    @Test
    void 양식은_예전_양식과_같은_28열_순서이고_샘플과_안내_시트가_있다() throws Exception {
        assertEquals(28, ALL_HEADERS.length);
        assertEquals("주문번호", ALL_HEADERS[0]);
        assertEquals("수취인", ALL_HEADERS[6]);
        assertEquals("주문 일자", ALL_HEADERS[21]);
        assertEquals("판촉비", ALL_HEADERS[27]);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(service.buildTemplate()))) {
            assertEquals("Sheet1", wb.getSheetAt(0).getSheetName());
            assertEquals("작성안내", wb.getSheetAt(1).getSheetName());
            Row h = wb.getSheetAt(0).getRow(0);
            assertEquals(28, h.getLastCellNum());
            for (int i = 0; i < 28; i++) assertEquals(ALL_HEADERS[i], h.getCell(i).getStringCellValue());
            assertNotNull(wb.getSheetAt(0).getRow(1), "샘플 행");
        }
    }

    @Test
    void 양식에_적힌_샘플_행은_그대로_올려도_검사를_통과한다() throws Exception {
        when(mapper.selectChnlCodesByName(eq("홈앤쇼핑"), any())).thenReturn(List.of("CH1"));
        OrderExcelResult r = service.preview(new ByteArrayInputStream(service.buildTemplate()), request);

        assertNull(r.getFileError());
        assertEquals(1, r.getRows().size());
        assertEquals(List.of(), r.getRows().get(0).getErrors());
    }

    // ── 읽기 ────────────────────────────────────────────────

    @Test
    void 정상_행은_검사만_하고_아무것도_저장하지_않는다() throws Exception {
        OrderExcelResult r = check(good());

        assertNull(r.getFileError());
        assertEquals(1, r.getOkCount());
        assertEquals(0, r.getErrorCount());
        assertTrue(r.isImportable());
        OrderExcelRow row = r.getRows().get(0);
        assertEquals(OrderExcelRow.OK, row.getStatus());
        assertEquals(2, row.getRowNo());
        assertEquals("T001", row.getExcelOrderNo());
        assertEquals("CH1", row.getParsed().getChnlCode());
        assertEquals(1000, row.getParsed().getSalePrice());
        verifyNoInteractions(orderService, orderProdService, orderPayService, orderNoSeqMapper, stockLedgerService);
        verify(mapper, never()).insertUpload(any());
        verify(mapper, never()).insertExcelOrder(any());
    }

    @Test
    void 열_순서가_달라도_헤더_이름으로_읽는다() throws Exception {
        String[] reversed = ALL_HEADERS.clone();
        Collections.reverse(Arrays.asList(reversed));
        Object[] g = good();
        Object[] flipped = new Object[g.length];
        for (int i = 0; i < g.length; i++) flipped[g.length - 1 - i] = g[i];

        OrderExcelResult r = service.preview(new ByteArrayInputStream(book(false, reversed, List.<Object[]>of(flipped))), request);

        assertEquals(List.of(), r.getRows().get(0).getErrors());
        assertEquals("T001", r.getRows().get(0).getExcelOrderNo());
    }

    @Test
    void xlsx_도_읽는다() throws Exception {
        OrderExcelResult r = service.preview(new ByteArrayInputStream(book(true, ALL_HEADERS, List.<Object[]>of(good()))), request);

        assertNull(r.getFileError());
        assertEquals(1, r.getOkCount());
    }

    @Test
    void 필수_열이_없으면_파일_오류로_어떤_열인지_알려_준다() throws Exception {
        String[] headers = Arrays.stream(ALL_HEADERS).filter(h -> !h.equals("판매 가격") && !h.equals("채널명")).toArray(String[]::new);
        OrderExcelResult r = service.preview(new ByteArrayInputStream(book(false, headers, List.<Object[]>of(new Object[]{"x"}))), request);

        assertNotNull(r.getFileError());
        assertTrue(r.getFileError().contains("판매 가격") && r.getFileError().contains("채널명"), r.getFileError());
        assertTrue(r.getRows().isEmpty());
        assertFalse(r.isImportable());
    }

    @Test
    void 엑셀이_아닌_파일과_데이터가_없는_파일은_파일_오류다() throws Exception {
        assertNotNull(service.preview(new ByteArrayInputStream("not excel".getBytes()), request).getFileError());
        assertTrue(check().getFileError().contains("등록할 주문 행이 없습니다"));
    }

    @Test
    void 행이_300을_넘으면_파일_오류다() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < OrderExcelService.MAX_ROWS + 1; i++) rows.add(good("주문번호", "N" + i));
        OrderExcelResult r = service.preview(new ByteArrayInputStream(book(false, ALL_HEADERS, rows)), request);

        assertTrue(r.getFileError().contains(String.valueOf(OrderExcelService.MAX_ROWS)), r.getFileError());
    }

    @Test
    void 완전히_빈_행은_건너뛰고_행_번호는_엑셀_기준이다() throws Exception {
        OrderExcelResult r = check(good("주문번호", "A"), new Object[ALL_HEADERS.length], good("주문번호", "B"));

        assertEquals(2, r.getRows().size());
        assertEquals(2, r.getRows().get(0).getRowNo());
        assertEquals(4, r.getRows().get(1).getRowNo());
    }

    // ── 행 검증 ─────────────────────────────────────────────

    @Test
    void 필수값이_빠지면_오류를_모두_모아서_보여_준다() throws Exception {
        OrderExcelResult r = check(row("주문번호", "T9", "채널명", "홈앤쇼핑"));

        OrderExcelRow row = r.getRows().get(0);
        assertEquals(OrderExcelRow.ERROR, row.getStatus());
        String all = String.join("|", row.getErrors());
        for (String must : new String[]{"채널주문번호", "주문자명", "수취인", "수취인 전화번호나 휴대번호", "수취인 주소", "상품 명칭",
                "공급 가격", "판매 가격", "주문 수량", "주문 일자", "배송비 타입"}) {
            assertTrue(all.contains(must), must + " 오류가 있어야 합니다: " + all);
        }
        assertTrue(row.getErrors().size() >= 10);
        assertNull(row.getParsed());
        assertEquals(0, r.getOkCount());
        assertFalse(r.isImportable());
    }

    @Test
    void 숫자_칸은_쉼표_소수점0_원_표기를_받아들이고_0원도_허용한다() throws Exception {
        OrderExcelResult r = check(good("공급 가격", "1,000", "판매 가격", "52900.0", "매입 가격", "0", "배송비", "3,000원"));

        OrderExcelRow row = r.getRows().get(0);
        assertEquals(List.of(), row.getErrors());
        assertEquals(1000, row.getParsed().getSupplyPrice());
        assertEquals(52900, row.getParsed().getSalePrice());
        assertEquals(0, row.getParsed().getBuyPrice());
        assertEquals(3000, row.getParsed().getDeliFeeAmt());
    }

    @Test
    void 판매가격이_0원이면_등록은_되지만_경고가_표시된다() throws Exception {
        OrderExcelResult r = check(good("판매 가격", 0), good("주문번호", "T002"));

        OrderExcelRow zero = r.getRows().get(0);
        assertEquals(OrderExcelRow.OK, zero.getStatus());
        assertEquals(1, zero.getWarnings().stream().filter(w -> w.contains("판매 가격이 0원")).count());
        assertTrue(r.isImportable());
        // 0원이 아닌 행에는 이 경고가 없다. 공급·매입 가격이 0인 것은 경고하지 않는다(마스터 공급가/선택 열).
        assertTrue(r.getRows().get(1).getWarnings().stream().noneMatch(w -> w.contains("0원")));
        assertTrue(check(good("공급 가격", 0, "매입 가격", 0)).getRows().get(0).getWarnings().stream().noneMatch(w -> w.contains("0원")));
    }

    @Test
    void 숫자가_아니거나_음수_소수_범위_밖이면_그_행의_오류다() throws Exception {
        for (Object[] bad : new Object[][]{
                {"판매 가격", "천원"}, {"판매 가격", -1}, {"판매 가격", 10.5}, {"주문 수량", 0}, {"주문 수량", 1000},
                {"주문 수량", 1.5}, {"공급 가격", "abc"}, {"배송비", -5}, {"판촉비", "x"}}) {
            OrderExcelRow row = check(good(bad)).getRows().get(0);
            assertEquals(OrderExcelRow.ERROR, row.getStatus(), Arrays.toString(bad));
            assertTrue(row.getErrors().stream().anyMatch(e -> e.contains(String.valueOf(bad[0]))), Arrays.toString(bad) + " " + row.getErrors());
        }
    }

    @Test
    void 단가에_수량을_곱해_int_범위를_넘으면_오류다() throws Exception {
        OrderExcelRow row = check(good("판매 가격", 2_000_000_000, "주문 수량", 5)).getRows().get(0);

        assertTrue(row.getErrors().stream().anyMatch(e -> e.contains("너무 큽니다")), row.getErrors().toString());
    }

    @Test
    void 주문일자는_여러_표기를_받고_미래는_오류_오래되면_경고다() throws Exception {
        LocalDate d = LocalDate.now().minusDays(3);
        for (Object v : new Object[]{d.toString(), d.toString().replace('-', '/'), d.toString().replace('-', '.'),
                d.toString().replace("-", ""), d + " 00:00:00.0"}) {
            OrderExcelRow row = check(good("주문 일자", v)).getRows().get(0);
            assertEquals(List.of(), row.getErrors(), String.valueOf(v));
            assertEquals(d, row.getParsed().getOrderDate(), String.valueOf(v));
        }
        assertTrue(check(good("주문 일자", LocalDate.now().plusDays(1).toString())).getRows().get(0).getErrors().get(0).contains("오늘 이후"));
        assertTrue(check(good("주문 일자", "어제")).getRows().get(0).getErrors().get(0).contains("형식"));

        OrderExcelRow old = check(good("주문 일자", LocalDate.now().minusDays(60).toString())).getRows().get(0);
        assertEquals(List.of(), old.getErrors());
        assertEquals(1, old.getWarnings().stream().filter(w -> w.contains("오래")).count());
    }

    @Test
    void 날짜_서식_셀도_읽는다() throws Exception {
        Date yesterday = Date.from(LocalDate.now().minusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        OrderExcelRow row = check(good("주문 일자", yesterday)).getRows().get(0);

        assertEquals(List.of(), row.getErrors());
        assertEquals(LocalDate.now().minusDays(1), row.getParsed().getOrderDate());
    }

    @Test
    void 연락처는_휴대번호나_전화번호_중_하나면_된다() throws Exception {
        assertEquals(List.of(), check(good("수취인 휴대번호", null, "수취인 전화번호", "02-123-4567", "주문자 휴대번호", null))
                .getRows().get(0).getErrors());
        assertTrue(check(good("수취인 휴대번호", null)).getRows().get(0).getErrors().get(0).contains("수취인 전화번호"));
    }

    @Test
    void 길이_제한을_넘으면_오류다() throws Exception {
        String x51 = "가".repeat(51);
        assertFalse(check(good("주문자명", x51)).getRows().get(0).getErrors().isEmpty());
        assertFalse(check(good("채널주문번호", "1".repeat(51))).getRows().get(0).getErrors().isEmpty());
        assertFalse(check(good("주문번호", "1".repeat(21))).getRows().get(0).getErrors().isEmpty());
        assertFalse(check(good("수취인 주소", "가".repeat(256))).getRows().get(0).getErrors().isEmpty());
    }

    @Test
    void 채널주문번호_0과_상품코드_자리수를_검사한다() throws Exception {
        assertTrue(check(good("채널주문번호", "0")).getRows().get(0).getErrors().get(0).contains("채널주문번호"));
        assertTrue(check(good("상품 코드", "123")).getRows().get(0).getErrors().get(0).contains("10자리"));
        assertEquals(List.of(), check(good("상품 코드", "1000000001")).getRows().get(0).getErrors());
    }

    @Test
    void 배송비_타입은_레거시_규칙으로_선불_착불_무료가_된다() {
        assertEquals("선불", OrderExcelService.deliFeeType("선불"));
        assertEquals("선불", OrderExcelService.deliFeeType("선결제"));
        assertEquals("선불", OrderExcelService.deliFeeType("배송비:유료"));
        assertEquals("착불", OrderExcelService.deliFeeType("착불"));
        assertEquals("무료", OrderExcelService.deliFeeType("무료"));
        assertEquals("무료", OrderExcelService.deliFeeType("기타"));
        assertEquals("무료", OrderExcelService.deliFeeType(null));
    }

    // ── 채널 ────────────────────────────────────────────────

    @Test
    void 없는_채널과_이름이_겹치는_채널은_오류다() throws Exception {
        when(mapper.selectChnlCodesByName(eq("없는몰"), any())).thenReturn(List.of());
        when(mapper.selectChnlCodesByName(eq("겹치는몰"), any())).thenReturn(List.of("A", "B"));

        assertTrue(check(good("채널명", "없는몰")).getRows().get(0).getErrors().get(0).contains("찾을 수 없습니다"));
        assertTrue(check(good("채널명", "겹치는몰")).getRows().get(0).getErrors().get(0).contains("둘 이상"));
    }

    @Test
    void 채널_조회는_계정의_서비스_안에서만_하고_같은_이름은_한_번만_조회한다() throws Exception {
        when(scopeGuard.scope(any())).thenReturn(new DataScope(null, null, "SHP001"));
        when(mapper.selectChnlCodesByName("홈앤쇼핑", "SHP001")).thenReturn(List.of("CH1"));

        OrderExcelResult r = check(good("주문번호", "A"), good("주문번호", "B"), good("주문번호", "C"));

        assertEquals(3, r.getOkCount());
        verify(mapper, times(1)).selectChnlCodesByName("홈앤쇼핑", "SHP001");
    }

    @Test
    void 채널_제한_계정은_다른_채널_행이_오류다() throws Exception {
        when(scopeGuard.scope(any())).thenReturn(new DataScope(null, "OTHER", null));

        OrderExcelRow row = check(good()).getRows().get(0);

        assertEquals(OrderExcelRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("다른 채널"));
    }

    // ── 중복 ────────────────────────────────────────────────

    @Test
    void 이미_처리된_엑셀_주문번호는_오류가_아니라_중복이다() throws Exception {
        when(mapper.selectExistingExcelOrderNos(any())).thenReturn(List.of("T001"));

        OrderExcelResult r = check(good("주문번호", "T001"), good("주문번호", "T002"));

        assertEquals(OrderExcelRow.DUP, r.getRows().get(0).getStatus());
        assertEquals(OrderExcelRow.OK, r.getRows().get(1).getStatus());
        assertEquals(1, r.getDupCount());
        assertEquals(1, r.getOkCount());
        assertEquals(0, r.getErrorCount());
        assertTrue(r.isImportable(), "정상 행이 있으면 등록할 수 있다(중복은 건너뜀)");
    }

    @Test
    void 중복만_있으면_등록할_수_없다() throws Exception {
        when(mapper.selectExistingExcelOrderNos(any())).thenReturn(List.of("T001"));

        assertFalse(check(good("주문번호", "T001")).isImportable());
    }

    @Test
    void 같은_파일에서_주문번호가_반복되면_뒤_행은_오류다() throws Exception {
        OrderExcelResult r = check(good("주문번호", "T001"), good("주문번호", "T001"));

        assertEquals(OrderExcelRow.OK, r.getRows().get(0).getStatus());
        assertEquals(OrderExcelRow.ERROR, r.getRows().get(1).getStatus());
        assertTrue(r.getRows().get(1).getErrors().get(0).contains("두 번"));
    }

    // ── 상품 매칭 ───────────────────────────────────────────

    @Test
    void 상점상품코드로_매칭한다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));

        OrderExcelRow row = check(good()).getRows().get(0);

        assertEquals("1000000001", row.getParsed().getMatched().getProdCode());
        assertEquals(List.of(), row.getWarnings());
    }

    @Test
    void 상점상품코드로_못_찾으면_상품코드가_정확히_같은_상품으로_매칭한다() throws Exception {
        Prod other = prod("1000000002");
        Prod mine = prod("1000000001");
        when(prodService.selectList(Map.of("pProdCode", "1000000001"))).thenReturn(List.of(other, mine));

        OrderExcelRow row = check(good("상점 상품 코드", "없는코드", "상품 코드", "1000000001")).getRows().get(0);

        assertSame(mine, row.getParsed().getMatched());
    }

    @Test
    void 마지막으로_상품명과_옵션_규칙으로_매칭한다() throws Exception {
        ProdCodeMatch rule = new ProdCodeMatch();
        rule.setProdCode("1000000009");
        when(prodCodeMatchMapper.selectByNameOption("상품1", "옵션A")).thenReturn(rule);
        Prod byRule = prod("1000000009");
        when(prodService.selectList(Map.of("pProdCode", "1000000009"))).thenReturn(List.of(byRule));

        OrderExcelRow row = check(good("상점 상품 코드", null)).getRows().get(0);

        assertSame(byRule, row.getParsed().getMatched());
    }

    @Test
    void 매칭이_안_되면_경고만_하고_저장_대상이다() throws Exception {
        OrderExcelRow row = check(good()).getRows().get(0);

        assertEquals(OrderExcelRow.OK, row.getStatus());
        assertNull(row.getParsed().getMatched());
        assertEquals(1, row.getWarnings().size());
        assertTrue(row.getWarnings().get(0).contains("상품 매칭"));
    }

    @Test
    void 같은_상품은_한_번만_조회한다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));

        check(good("주문번호", "A"), good("주문번호", "B"), good("주문번호", "C"));

        verify(prodService, times(1)).selectByShopProdCode(any());
    }

    @Test
    void 다른_업체_상품에_매칭되면_그_행은_오류다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no")).when(scopeGuard).requirePrdProd(any(), eq("1000000001"));

        OrderExcelRow row = check(good()).getRows().get(0);

        assertEquals(OrderExcelRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("다른 업체"));
    }

    @Test
    void 상점_제한_계정은_상품이_연결되지_않은_행을_등록할_수_없다() throws Exception {
        when(scopeGuard.scope(any())).thenReturn(new DataScope("SHOP1", null, null));

        OrderExcelRow row = check(good()).getRows().get(0);

        assertEquals(OrderExcelRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("상품이 연결되지 않아"));
    }

    // ── 등록 ────────────────────────────────────────────────

    @Test
    void 매칭된_행은_지불완료_엑셀_결제로_저장하고_금액은_수량을_곱한_합계다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));
        LocalDate date = LocalDate.now().minusDays(2);

        OrderExcelResult r = run(good("주문 일자", date.toString(), "배송비 타입", "선결제", "배송비", 2500, "판촉비", 100));

        assertTrue(r.isImported());
        assertEquals(1, r.getCreatedCount());
        assertEquals(800001, r.getRows().get(0).getOrderNo());

        ArgumentCaptor<Order> oc = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(oc.capture());
        Order o = oc.getValue();
        assertEquals(800001, o.getOrderNo());
        assertEquals("지불완료", o.getOrderState());
        assertEquals(0, o.getCustId());
        assertEquals("CH1", o.getChnlCode());
        assertEquals(date + " 00:00:00", o.getOrderReciptDate());   // 엑셀의 주문일자가 주문접수일
        assertNotNull(o.getPayCmpletDate());
        assertEquals("", o.getRecverAddr2());
        assertEquals("서울 중구 세종대로 110", o.getRecverAddr1());
        assertEquals("010-3333-4444", o.getRecverMobileNo());
        assertEquals("문 앞", o.getDeliMemo());
        assertEquals("SYSTEM", o.getRegistId());   // 이 테스트의 요청에는 로그인 정보가 없다

        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        OrderProd op = pc.getValue();
        assertEquals(1, op.getOrderProdNo());
        assertEquals("지불완료", op.getOrderState());
        assertEquals("옵션A", op.getItemName());
        assertEquals("CO-1", op.getChnlOrderNo());
        assertEquals(2, op.getProdQty());
        assertEquals(2000, op.getSalePrice());      // 1,000 × 2
        assertEquals(2000, op.getListPrice());
        assertEquals(2000, op.getReciptAmt());
        assertEquals(0, op.getNvPrice());
        assertEquals(1800, op.getSupplyPrice());    // 엑셀 공급가 900 × 2 (0보다 크면 채널 공급가 유지)
        assertEquals(1200, op.getBuyPrice());       // 매칭되면 상품 마스터 600 × 2
        assertEquals(800, op.getProdMargin());      // 2000 − 1200
        assertEquals(200, op.getChnlMargin());      // 2000 − 1800
        assertEquals(500, op.getNetMargin());       // 1800 − 1200 − 판촉비 100
        assertEquals("선불", op.getDeliFeeType());
        assertEquals(2500, op.getDeliFeeAmt());
        assertEquals(100, op.getPromotFeeAmt());
        assertEquals("1000000001", op.getProdCode());
        assertEquals("SHOP1", op.getShopCode());
        assertEquals(10, op.getVatRate());

        ArgumentCaptor<OrderPay> payc = ArgumentCaptor.forClass(OrderPay.class);
        verify(orderPayService).insert(payc.capture());
        OrderPay pay = payc.getValue();
        assertEquals(1, pay.getPayNo());
        assertEquals("E00000000000", pay.getPayType());
        assertEquals("EXCEL", pay.getPgCode());
        assertEquals(2000, pay.getPayAmt());
        assertEquals("CO-1", pay.getPgOrderNo());

        // 재고: 원장 선점 후 차감
        verify(stockLedgerService).claimDeduction(800001, 1, "1000000001", 2, "SYSTEM");
        ArgumentCaptor<Map<String, Object>> dec = ArgumentCaptor.forClass(Map.class);
        verify(prodService).updateSupplyQty(dec.capture());
        assertEquals("1000000001", dec.getValue().get("prodCode"));
        assertEquals(2, dec.getValue().get("qty"));
    }

    @Test
    void 원본을_보관하고_이력을_남긴다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));

        OrderExcelResult r = run(good());

        ArgumentCaptor<Map<String, Object>> sc = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertExcelOrder(sc.capture());
        Map<String, Object> staged = sc.getValue();
        assertEquals("T001", staged.get("orderNo"));          // 중복 판정의 기준은 엑셀 주문번호
        assertEquals("800001", staged.get("shopionOrderNo"));
        assertEquals("홈앤쇼핑", staged.get("chnlName"));
        assertEquals("1000", staged.get("salePrice"));         // 원본 보관은 엑셀의 1개 단가 문자열 그대로
        assertEquals("2", staged.get("prodQty"));
        assertEquals("무료", staged.get("deliFeeType"));
        assertEquals("", staged.get("orderPhoneNo"));          // 빈 칸은 null 이 아니라 ''

        assertEquals(555, r.getUploadId());
        ArgumentCaptor<OrderExcelUpload> up = ArgumentCaptor.forClass(OrderExcelUpload.class);
        verify(mapper).insertUpload(up.capture());
        assertEquals("orders.xls", up.getValue().getFileLoc());
        verify(mapper).finishUpload(eq(555), eq("성공"), eq("처리를 완료하였습니다. 중복 - 0건, 오류 - 0건, 성공 - 1건"), anyString(), anyString());
        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(mapper).insertUploadDetail(dc.capture());
        assertEquals("성공", dc.getValue().getResultType());
        assertEquals(2, dc.getValue().getUploadDetailNo());
        assertEquals("T001", dc.getValue().getExcelOrderNo());
        assertTrue(dc.getValue().getResultMessag().contains("800001"));
    }

    @Test
    void 매칭되지_않은_행은_저장하되_재고는_건드리지_않고_엑셀_매입가를_쓴다() throws Exception {
        run(good());

        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        OrderProd op = pc.getValue();
        assertNull(op.getProdCode());
        assertEquals(1400, op.getBuyPrice());       // 700 × 2
        assertEquals(1800, op.getSupplyPrice());
        assertEquals(600, op.getProdMargin());      // 2000 − 1400
        assertEquals(200, op.getChnlMargin());
        assertEquals(400, op.getNetMargin());       // 1800 − 1400
        verifyNoInteractions(stockLedgerService);
        verify(prodService, never()).updateSupplyQty(any());
    }

    @Test
    void 엑셀_공급가가_0이면_상품_마스터_공급가를_쓴다() throws Exception {
        givenSlugProd("SP-1", prod("1000000001"));

        run(good("공급 가격", 0));

        ArgumentCaptor<OrderProd> pc = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdService).insert(pc.capture());
        assertEquals(1600, pc.getValue().getSupplyPrice());   // 마스터 800 × 2
    }

    @Test
    void 정상_행만_저장하고_오류와_중복_행은_건너뛰되_이력에는_모두_남긴다() throws Exception {
        when(mapper.selectExistingExcelOrderNos(any())).thenReturn(List.of("DUP1"));

        OrderExcelResult r = run(good("주문번호", "OK1"), good("주문번호", "DUP1"), good("주문번호", "BAD1", "판매 가격", "x"),
                good("주문번호", "OK2"));

        assertEquals(2, r.getCreatedCount());
        assertEquals(1, r.getDupCount());
        assertEquals(1, r.getErrorCount());
        assertEquals("처리를 완료하였습니다. 중복 - 1건, 오류 - 1건, 성공 - 2건", r.getMessage());
        verify(orderService, times(2)).insert(any());
        verify(mapper, times(2)).insertExcelOrder(any());

        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(mapper, times(4)).insertUploadDetail(dc.capture());
        List<OrderExcelUploadDetail> d = dc.getAllValues();
        assertEquals(List.of("성공", "중복", "오류", "성공"), d.stream().map(OrderExcelUploadDetail::getResultType).toList());
        assertEquals(List.of(2, 3, 4, 5), d.stream().map(OrderExcelUploadDetail::getUploadDetailNo).toList());
        assertTrue(d.get(2).getResultMessag().contains("판매 가격"), "검증 오류도 이력에 남는다(레거시는 안 남김)");
        assertEquals("이미 처리된 건과 중복입니다.", d.get(1).getResultMessag());
    }

    @Test
    void 정상_행이_없으면_아무것도_만들지_않고_이력도_남기지_않는다() throws Exception {
        when(mapper.selectExistingExcelOrderNos(any())).thenReturn(List.of("T001"));

        OrderExcelResult r = run(good());

        assertFalse(r.isImported());
        assertNull(r.getUploadId());
        verify(mapper, never()).insertUpload(any());
        verifyNoInteractions(orderService);
    }

    @Test
    void 파일_오류면_등록하지_않는다() throws Exception {
        OrderExcelResult r = service.importFile(new ByteArrayInputStream("nope".getBytes()), "x.xls", request);

        assertNotNull(r.getFileError());
        verify(mapper, never()).insertUpload(any());
    }

    @Test
    void 등록_도중_같은_주문번호가_먼저_들어가면_그_행만_중복으로_본다() throws Exception {
        doThrow(new DuplicateKeyException("dup")).doReturn(1).when(mapper).insertExcelOrder(any());

        OrderExcelResult r = run(good("주문번호", "A"), good("주문번호", "B"));

        assertEquals(OrderExcelRow.DUP, r.getRows().get(0).getStatus());
        assertNull(r.getRows().get(0).getOrderNo());
        assertEquals(OrderExcelRow.OK, r.getRows().get(1).getStatus());
        assertEquals(1, r.getCreatedCount());
        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(mapper, times(2)).insertUploadDetail(dc.capture());
        assertEquals("중복", dc.getAllValues().get(0).getResultType());
    }

    @Test
    void 한_행의_저장_실패가_다른_행에_영향을_주지_않는다() throws Exception {
        doThrow(new IllegalStateException("db down")).doNothing().when(orderPayService).insert(any());

        OrderExcelResult r = run(good("주문번호", "A"), good("주문번호", "B"));

        OrderExcelRow first = r.getRows().get(0);
        assertEquals(OrderExcelRow.ERROR, first.getStatus());
        assertTrue(first.getErrors().get(0).contains("저장 중 오류"));
        assertNull(first.getOrderNo());
        assertEquals(OrderExcelRow.OK, r.getRows().get(1).getStatus());
        assertNotNull(r.getRows().get(1).getOrderNo());
        assertEquals(1, r.getCreatedCount());
        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(mapper, times(2)).insertUploadDetail(dc.capture());
        assertEquals("오류", dc.getAllValues().get(0).getResultType());
        assertEquals("성공", dc.getAllValues().get(1).getResultType());
    }

    @Test
    void 로그인한_사용자가_등록자로_기록된다() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("svcAll", "Y", "loginId", "rohhd", "name", "노학동"));
        AuthSession.markActor(req, AuthSession.Type.MNGR);

        service.importFile(xls(good()), "orders.xls", req);

        ArgumentCaptor<Order> oc = ArgumentCaptor.forClass(Order.class);
        verify(orderService).insert(oc.capture());
        assertEquals("rohhd", oc.getValue().getRegistId());
        assertEquals("노학동", oc.getValue().getRegistName());
        ArgumentCaptor<OrderExcelUpload> up = ArgumentCaptor.forClass(OrderExcelUpload.class);
        verify(mapper).insertUpload(up.capture());
        assertEquals("rohhd", up.getValue().getRegistId());
    }

    // ── 이력 조회 ───────────────────────────────────────────

    private static MockHttpServletRequest loggedIn(String loginId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("svcAll", "Y", "loginId", loginId, "name", loginId));
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    @Test
    void 제한_없는_계정은_모든_이력을_보고_쪽수는_범위로_고정된다() {
        service.uploads(Map.of("pStartDate", "2026-11-01", "pEndDate", "2026-11-30", "pPageOffset", "-5", "pPageSize", "9999"), loggedIn("admin"));

        verify(mapper).selectUploadList("2026-11-01", "2026-11-30", null, 0, 100);
    }

    @Test
    void 상점_채널_제한_계정은_자기가_올린_이력만_본다() {
        when(scopeGuard.scope(any())).thenReturn(new DataScope("SHOP1", null, null));

        service.uploads(Map.of(), loggedIn("shopUser"));
        service.uploadCount(Map.of(), loggedIn("shopUser"));

        verify(mapper).selectUploadList(null, null, "shopUser", 0, 20);
        verify(mapper).selectUploadCount(null, null, "shopUser");
    }

    @Test
    void 제한_계정은_남이_올린_이력의_상세를_볼_수_없다() {
        when(scopeGuard.scope(any())).thenReturn(new DataScope("SHOP1", null, null));
        OrderExcelUpload mine = new OrderExcelUpload();
        mine.setRegistId("shopUser");
        OrderExcelUpload theirs = new OrderExcelUpload();
        theirs.setRegistId("someoneElse");
        when(mapper.selectUpload(1)).thenReturn(mine);
        when(mapper.selectUpload(2)).thenReturn(theirs);

        service.uploadDetails(1, loggedIn("shopUser"));
        verify(mapper).selectUploadDetails(1);
        assertThrows(ResponseStatusException.class, () -> service.uploadDetails(2, loggedIn("shopUser")));
        assertThrows(ResponseStatusException.class, () -> service.uploadDetails(3, loggedIn("shopUser")));
        verify(mapper, never()).selectUploadDetails(2);
    }
}
