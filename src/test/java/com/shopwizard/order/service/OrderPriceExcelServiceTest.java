package com.shopwizard.order.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.order.mapper.OrderExcelMapper;
import com.shopwizard.order.mapper.OrderPriceMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderExcelUpload;
import com.shopwizard.order.model.OrderExcelUploadDetail;
import com.shopwizard.order.model.OrderPriceResult;
import com.shopwizard.order.model.OrderPriceRow;
import com.shopwizard.order.model.OrderProd;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 주문 엑셀 가격수정 — 양식·검증·계산(마진·부가세)·거절 규칙·사본/정산자료 동기화·이력. 메모리에서 만든 .xls/.xlsx 로 시험한다. */
class OrderPriceExcelServiceTest {

    private static final String[] ALL_HEADERS = Arrays.stream(OrderPriceExcelService.COLUMNS).map(c -> c[0]).toArray(String[]::new);

    private final OrderPriceMapper priceMapper = mock(OrderPriceMapper.class);
    private final OrderProdMapper orderProdMapper = mock(OrderProdMapper.class);
    private final OrderExcelMapper historyMapper = mock(OrderExcelMapper.class);
    private final OrderLineLock lineLock = mock(OrderLineLock.class);
    private final DataScopeGuard scopeGuard = mock(DataScopeGuard.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private OrderPriceExcelService service;
    private OrderProd line;
    private Map<String, Object> guard;

    @BeforeEach
    void setUp() {
        service = new OrderPriceExcelService(priceMapper, orderProdMapper, historyMapper, lineLock, scopeGuard, txManager);
        line = line(1001, 1, "지불완료");
        guard = guard("E00000000000", 0, 0, 0, 0);
        when(orderProdMapper.selectLineRaw(anyMap())).thenAnswer(inv -> lineFor(inv.getArgument(0)));
        when(lineLock.lockLine(anyInt(), anyInt())).thenAnswer(inv -> lineFor(Map.of("orderNo", inv.getArgument(0), "orderProdNo", inv.getArgument(1))));
        when(priceMapper.selectGuard(anyInt(), anyInt())).thenAnswer(inv -> guard);
        when(priceMapper.updateLine(anyMap())).thenReturn(1);
        doAnswer(inv -> {
            ((OrderExcelUpload) inv.getArgument(0)).setUploadId(777);
            return 1;
        }).when(historyMapper).insertUpload(any());
    }

    /** 키가 맞는 줄만 돌려준다(기본: 1001-1). */
    private OrderProd lineFor(Map<String, Object> key) {
        return line != null && Objects.equals(line.getOrderNo(), key.get("orderNo")) && Objects.equals(line.getOrderProdNo(), key.get("orderProdNo")) ? line : null;
    }

    /** 엑셀 업로드로 들어온 줄 모양: 수량 2, 옵션가 합계 200, 판매 2000(+옵션 200 = 영수 2200), 공급 1800, 원가 1400. */
    private static OrderProd line(int orderNo, int orderProdNo, String state) {
        OrderProd l = new OrderProd();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(orderProdNo);
        l.setOrderState(state);
        l.setProdName("상품1");
        l.setShopProdCode("SP-1");
        l.setProdQty(2);
        l.setNvPrice(200);
        l.setSalePrice(2000);
        l.setReciptAmt(2200);
        l.setSupplyPrice(1800);
        l.setBuyPrice(1400);
        l.setVatRate(10);
        l.setVatAmt(1818);
        l.setProdMargin(800);
        l.setChnlMargin(400);
        l.setNetMargin(400);
        l.setPromotFeeAmt(0);
        l.setDeliFeeType("무료");
        l.setDeliFeeAmt(0);
        return l;
    }

    private static Map<String, Object> guard(String payType, long closed, long open, long shipCopy, long returnCopy) {
        Map<String, Object> g = new HashMap<>();
        g.put("payType", payType);
        g.put("closedCnt", closed);
        g.put("openCnt", open);
        g.put("shipCopyCnt", shipCopy);
        g.put("returnCopyCnt", returnCopy);
        return g;
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

    private OrderPriceResult check(Object[]... rows) throws Exception {
        return service.preview(xls(rows), request);
    }

    private OrderPriceResult run(Object[]... rows) throws Exception {
        return service.importFile(xls(rows), "price.xls", request);
    }

    // ── 양식 ────────────────────────────────────────────────

    @Test
    void 양식은_레거시_헤더_이름이고_샘플과_안내_시트가_있다() throws Exception {
        assertEquals(List.of("주문번호", "순번", "상점상품코드", "상품명", "상품옵션", "수량", "판매액", "채널공급가", "원가", "판촉비", "배송형태", "배송비"),
                List.of(ALL_HEADERS));
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(service.buildTemplate()))) {
            assertEquals("Sheet1", wb.getSheetAt(0).getSheetName());
            assertEquals("작성안내", wb.getSheetAt(1).getSheetName());
            Row h = wb.getSheetAt(0).getRow(0);
            for (int i = 0; i < ALL_HEADERS.length; i++) assertEquals(ALL_HEADERS[i], h.getCell(i).getStringCellValue());
            assertNotNull(wb.getSheetAt(0).getRow(1), "샘플 행");
        }
    }

    // ── 읽기 ────────────────────────────────────────────────

    @Test
    void 판매액을_고치면_줄합계에서_옵션가를_빼고_마진과_부가세를_새_가격으로_계산한다() throws Exception {
        OrderPriceResult r = check(row("주문번호", 1001, "순번", 1, "판매액", 1200));

        OrderPriceRow row = r.getRows().get(0);
        assertEquals(OrderPriceRow.OK, row.getStatus());
        assertEquals("상품1", row.getProdName());
        assertEquals("지불완료", row.getOrderState());
        assertEquals(List.of("판매 줄합계 2,200→2,400"), row.getChanges());
        assertEquals(1, r.getOkCount());
        assertTrue(r.isImportable());

        OrderPriceExcelService.Plan p = OrderPriceExcelService.plan(line, row.getInput());
        assertEquals(2200, p.sale);                 // 1,200 × 2 − 옵션가 200
        assertEquals(2000, p.vatAmt);               // 2200×100/110 (새 가격 기준)
        assertEquals(1000, p.prodMargin);           // (2200+200) − 원가 1400
        assertEquals(600, p.chnlMargin);            // 2400 − 공급 1800
        assertEquals(400, p.netMargin);             // 공급 1800 − 원가 1400 − 판촉비 0
        assertEquals(2400, p.reciptAmt);            // 영수금액이 있는 줄은 판매액에 맞춘다
        assertEquals(1800, p.supply);               // 안 적은 값은 그대로
        assertEquals(1400, p.buy);
    }

    @Test
    void 공급가와_원가는_단가에_수량을_곱하고_판촉비와_배송비는_줄_값_그대로다() throws Exception {
        OrderPriceRow row = check(row("주문번호", 1001, "채널공급가", 1000, "원가", 600, "판촉비", 50, "배송비", 2500, "배송형태", "선결제")).getRows().get(0);

        assertEquals(OrderPriceRow.OK, row.getStatus());
        assertEquals(List.of("공급 1,800→2,000", "원가 1,400→1,200", "판촉비 0→50", "배송비 0→2,500", "배송형태 무료→선불"), row.getChanges());
        OrderPriceExcelService.Plan p = OrderPriceExcelService.plan(line, row.getInput());
        assertEquals(2000, p.supply);
        assertEquals(1200, p.buy);
        assertEquals(50, p.promotFeeAmt);
        assertEquals(2500, p.deliFeeAmt);
        assertEquals("선불", p.deliFeeType);
        assertEquals(2000 - 1200 - 50, p.netMargin);
        assertEquals(2200 - 1200, p.prodMargin);        // 판매 안 바꿈: (2000+200) − 원가
        assertEquals(2200 - 2000, p.chnlMargin);
        assertEquals(1818, p.vatAmt);                   // 판매가 그대로면 부가세액도 그대로
        assertNull(p.reciptAmt);
    }

    @Test
    void 빈_칸은_현재_값을_유지하고_바뀌는_값이_없으면_변경없음이다() throws Exception {
        OrderPriceResult r = check(row("주문번호", 1001), row("주문번호", 1001, "순번", 1, "판매액", 1100, "채널공급가", 900, "원가", 700, "배송형태", "무료", "배송비", 0));
        // 두 번째 행은 첫 행과 같은 키라 오류(같은 값 확인은 아래에서 따로)
        assertEquals(OrderPriceRow.SAME, r.getRows().get(0).getStatus());
        assertEquals(1, r.getSameCount());
        assertEquals(OrderPriceRow.ERROR, r.getRows().get(1).getStatus());

        OrderPriceResult same = check(row("주문번호", 1001, "판매액", 1100, "채널공급가", 900, "원가", 700, "배송형태", "무료", "배송비", 0, "판촉비", 0));
        assertEquals(OrderPriceRow.SAME, same.getRows().get(0).getStatus());   // 1100×2−200 = 2000, 900×2 = 1800, 700×2 = 1400
        assertFalse(same.isImportable());
    }

    @Test
    void 순번을_비우면_1이고_쉼표_소수점_원_표기를_받는다() throws Exception {
        OrderPriceRow row = check(row("주문번호", "1001", "판매액", "1,200.0", "채널공급가", "1,000원")).getRows().get(0);

        assertEquals(1, row.getOrderProdNo());
        assertEquals(OrderPriceRow.OK, row.getStatus());
        assertEquals(1200, row.getInput().getSaleUnit());
        assertEquals(1000, row.getInput().getSupplyUnit());
        assertEquals("1001-1", row.getKey());
    }

    @Test
    void 수량이_주문과_다르면_오류이고_비우거나_같으면_통과한다() throws Exception {
        OrderPriceRow diff = check(row("주문번호", 1001, "수량", 3, "판매액", 1200)).getRows().get(0);
        assertEquals(OrderPriceRow.ERROR, diff.getStatus());
        assertTrue(diff.getErrors().get(0).contains("수량이 주문과 다릅니다"));

        assertEquals(OrderPriceRow.OK, check(row("주문번호", 1001, "수량", 2, "판매액", 1200)).getRows().get(0).getStatus());
        assertEquals(OrderPriceRow.OK, check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0).getStatus());
    }

    @Test
    void 상점상품코드가_다르면_경고만_한다() throws Exception {
        OrderPriceRow row = check(row("주문번호", 1001, "상점상품코드", "OTHER-9", "판매액", 1200)).getRows().get(0);

        assertEquals(OrderPriceRow.OK, row.getStatus());
        assertTrue(row.getWarnings().stream().anyMatch(w -> w.contains("상점상품코드") && w.contains("상품은 바꾸지 않습니다")));
    }

    @Test
    void 판매액이_옵션가보다_작으면_오류다() throws Exception {
        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 50)).getRows().get(0);   // 50×2 = 100 < 옵션가 200

        assertEquals(OrderPriceRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("옵션가보다 작습니다"));
    }

    @Test
    void 숫자가_아니거나_음수_소수면_그_행의_오류다() throws Exception {
        for (Object[] bad : new Object[][]{{"판매액", "천원"}, {"판매액", -1}, {"채널공급가", 10.5}, {"원가", "abc"}, {"판촉비", -3}, {"배송비", "x"}, {"수량", "많이"}}) {
            Object[] kv = new Object[]{"주문번호", 1001, bad[0], bad[1]};
            OrderPriceRow row = check(row(kv)).getRows().get(0);
            assertEquals(OrderPriceRow.ERROR, row.getStatus(), Arrays.toString(bad));
            assertTrue(row.getErrors().stream().anyMatch(e -> e.contains(String.valueOf(bad[0]))), Arrays.toString(bad) + " " + row.getErrors());
        }
    }

    @Test
    void 금액이_int_범위를_넘으면_오류다() throws Exception {
        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 2_000_000_000)).getRows().get(0);

        assertEquals(OrderPriceRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("너무 큽니다"));
    }

    @Test
    void 주문번호가_없거나_줄을_못_찾으면_오류다() throws Exception {
        assertTrue(check(row("순번", 1, "판매액", 1)).getRows().get(0).getErrors().get(0).contains("주문번호"));
        OrderPriceRow missing = check(row("주문번호", 9999, "판매액", 1)).getRows().get(0);
        assertEquals(OrderPriceRow.ERROR, missing.getStatus());
        assertTrue(missing.getErrors().get(0).contains("주문 줄을 찾을 수 없습니다"));
        assertTrue(check(row("주문번호", 1001, "순번", 2, "판매액", 1)).getRows().get(0).getErrors().get(0).contains("찾을 수 없습니다"));
    }

    @Test
    void 같은_줄이_파일에_두_번_나오면_뒤_행은_오류다() throws Exception {
        OrderPriceResult r = check(row("주문번호", 1001, "판매액", 1200), row("주문번호", 1001, "순번", 1, "판매액", 1300));

        assertEquals(OrderPriceRow.OK, r.getRows().get(0).getStatus());
        assertEquals(OrderPriceRow.ERROR, r.getRows().get(1).getStatus());
        assertTrue(r.getRows().get(1).getErrors().get(0).contains("두 번"));
    }

    @Test
    void 데이터_범위_밖의_줄은_오류다() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no")).when(scopeGuard).requireLine(any(), eq(1001), eq(1));

        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);

        assertEquals(OrderPriceRow.ERROR, row.getStatus());
        assertTrue(row.getErrors().get(0).contains("다른 업체"));
        verifyNoInteractions(lineLock);
    }

    // ── 거절 규칙 ───────────────────────────────────────────

    @Test
    void 정산마감된_줄과_환불완료_주문접수_결제오류_줄은_고칠_수_없다() throws Exception {
        guard = guard("E00000000000", 1, 0, 1, 0);
        OrderPriceRow closed = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);
        assertEquals(OrderPriceRow.ERROR, closed.getStatus());
        assertTrue(closed.getErrors().stream().anyMatch(e -> e.contains("정산마감")));

        guard = guard("E00000000000", 0, 0, 0, 0);
        for (String state : new String[]{"환불완료", "주문접수", "결제오류", "알수없음"}) {
            line.setOrderState(state);
            OrderPriceRow r = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);
            assertEquals(OrderPriceRow.ERROR, r.getStatus(), state);
            assertTrue(r.getErrors().get(0).contains("'" + state + "' 상태"), r.getErrors().toString());
        }
    }

    @Test
    void 마감_전이면_출고지시_이후_모든_상태를_고칠_수_있다() throws Exception {
        for (String state : new String[]{"지불완료", "출고지시", "발주확인", "배송시작", "배송완료", "주문취소", "반품요청", "반품완료"}) {
            line.setOrderState(state);
            assertEquals(OrderPriceRow.OK, check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0).getStatus(), state);
        }
    }

    @Test
    void PG_결제_주문은_고칠_수_없고_샵링커_엑셀_수기_결제는_고칠_수_있다() throws Exception {
        for (String pg : new String[]{"100000000000", "001000000000", "010000000000", "SC0010"}) {
            guard = guard(pg, 0, 0, 0, 0);
            OrderPriceRow r = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);
            assertEquals(OrderPriceRow.ERROR, r.getStatus(), pg);
            assertTrue(r.getErrors().get(0).contains("PG"), pg);
        }
        for (String ok : new String[]{"D00000000000", "E00000000000", "M00000000000"}) {
            guard = guard(ok, 0, 0, 0, 0);
            assertEquals(OrderPriceRow.OK, check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0).getStatus(), ok);
        }
        guard = guard(null, 0, 0, 0, 0);   // 결제 행이 없으면 알 수 없으므로 거절
        assertEquals(OrderPriceRow.ERROR, check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0).getStatus());
    }

    @Test
    void 사본과_정산자료가_함께_바뀌면_경고로_알려_준다() throws Exception {
        guard = guard("E00000000000", 0, 2, 1, 1);
        line.setOrderState("배송시작");

        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);

        assertEquals(OrderPriceRow.OK, row.getStatus());
        String w = String.join("|", row.getWarnings());
        assertTrue(w.contains("출고 사본(1건)"), w);
        assertTrue(w.contains("마감 전 정산자료(2건)"), w);
        assertTrue(w.contains("반품 사본(1건)"), w);
    }

    @Test
    void 지불완료_줄의_원가를_바꾸면_출고지시에서_덮어쓰일_수_있다고_경고한다() throws Exception {
        OrderPriceRow ready = check(row("주문번호", 1001, "원가", 600)).getRows().get(0);
        assertTrue(ready.getWarnings().stream().anyMatch(w -> w.contains("출고지시 때 발주단가")));

        line.setOrderState("배송시작");
        OrderPriceRow shipped = check(row("주문번호", 1001, "원가", 600)).getRows().get(0);
        assertTrue(shipped.getWarnings().stream().noneMatch(w -> w.contains("발주단가")));
    }

    @Test
    void 영수금액이_없는_줄_샵링커_수집은_영수금액을_만들지_않는다() throws Exception {
        line.setReciptAmt(null);
        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);

        assertNull(OrderPriceExcelService.plan(line, row.getInput()).reciptAmt);
    }

    @Test
    void 부가세율이_0이면_부가세액은_0이다() throws Exception {
        line.setVatRate(0);
        line.setVatAmt(0);
        OrderPriceRow row = check(row("주문번호", 1001, "판매액", 1200)).getRows().get(0);

        assertEquals(0, OrderPriceExcelService.plan(line, row.getInput()).vatAmt);
    }

    // ── 파일 ────────────────────────────────────────────────

    @Test
    void xlsx_도_읽고_열_순서는_상관없다() throws Exception {
        String[] reversed = ALL_HEADERS.clone();
        Collections.reverse(Arrays.asList(reversed));
        Object[] r = row("주문번호", 1001, "판매액", 1200);
        Object[] flipped = new Object[r.length];
        for (int i = 0; i < r.length; i++) flipped[r.length - 1 - i] = r[i];

        OrderPriceResult res = service.preview(new ByteArrayInputStream(book(true, reversed, List.<Object[]>of(flipped))), request);

        assertEquals(OrderPriceRow.OK, res.getRows().get(0).getStatus());
    }

    @Test
    void 필수_열이_없거나_행이_없거나_300행을_넘으면_파일_오류다() throws Exception {
        String[] noKey = Arrays.stream(ALL_HEADERS).filter(h -> !h.equals("주문번호")).toArray(String[]::new);
        OrderPriceResult r = service.preview(new ByteArrayInputStream(book(false, noKey, List.<Object[]>of(new Object[]{1}))), request);
        assertTrue(r.getFileError().contains("주문번호"));
        assertFalse(r.isImportable());

        assertTrue(check().getFileError().contains("수정할 주문 행이 없습니다"));
        assertNotNull(service.preview(new ByteArrayInputStream("not excel".getBytes()), request).getFileError());

        List<Object[]> many = new ArrayList<>();
        for (int i = 0; i < OrderPriceExcelService.MAX_ROWS + 1; i++) many.add(row("주문번호", 5000 + i, "판매액", 1));
        assertTrue(service.preview(new ByteArrayInputStream(book(false, ALL_HEADERS, many)), request).getFileError()
                .contains(String.valueOf(OrderPriceExcelService.MAX_ROWS)));
    }

    // ── 수정(등록) ──────────────────────────────────────────

    @Test
    void 변경_행은_줄과_사본_정산자료를_한꺼번에_고치고_이력을_남긴다() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("svcAll", "Y", "loginId", "rohhd", "name", "노학동"));
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        guard = guard("E00000000000", 0, 1, 1, 0);
        line.setOrderState("배송시작");

        OrderPriceResult r = service.importFile(xls(row("주문번호", 1001, "판매액", 1200, "배송비", 3000)), "price.xls", req);

        assertTrue(r.isImported());
        assertEquals(1, r.getAppliedCount());
        assertTrue(r.getRows().get(0).isApplied());
        assertEquals(777, r.getUploadId());
        assertEquals("처리를 완료하였습니다. 변경없음 - 0건, 오류 - 0건, 성공 - 1건", r.getMessage());

        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(priceMapper).updateLine(cap.capture());
        Map<String, Object> p = cap.getValue();
        assertEquals(1001, p.get("orderNo"));
        assertEquals(1, p.get("orderProdNo"));
        assertEquals("배송시작", p.get("expectedState"));   // 상태가 그 사이 바뀌었으면 0행
        assertEquals(2, p.get("prodQty"));
        assertEquals(2200, p.get("salePrice"));
        assertEquals(1800, p.get("supplyPrice"));
        assertEquals(1400, p.get("buyPrice"));
        assertEquals(2000, p.get("vatAmt"));
        assertEquals(1000, p.get("prodMargin"));
        assertEquals(600, p.get("chnlMargin"));
        assertEquals(400, p.get("netMargin"));
        assertEquals(2400, p.get("reciptAmt"));
        assertEquals(3000, p.get("deliFeeAmt"));
        assertEquals("무료", p.get("deliFeeType"));
        assertEquals("rohhd", p.get("changeId"));
        assertEquals("노학동", p.get("changeName"));
        verify(priceMapper).updateShipCopy(p);
        verify(priceMapper).updateReturnCopy(p);
        verify(priceMapper).updateOpenShipDetail(p);
        verify(priceMapper).updateOpenReturnDetail(p);
        verify(lineLock).lockLine(1001, 1);

        ArgumentCaptor<OrderExcelUpload> up = ArgumentCaptor.forClass(OrderExcelUpload.class);
        verify(historyMapper).insertUpload(up.capture());
        assertEquals("[가격수정] price.xls", up.getValue().getFileLoc());
        assertEquals("rohhd", up.getValue().getRegistId());
        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(historyMapper).insertUploadDetail(dc.capture());
        assertEquals("1001-1", dc.getValue().getExcelOrderNo());
        assertEquals("성공", dc.getValue().getResultType());
        assertEquals(2, dc.getValue().getUploadDetailNo());
        assertTrue(dc.getValue().getResultMessag().contains("판매 줄합계 2,200→2,400"));
        verify(historyMapper).finishUpload(eq(777), eq("성공"), anyString(), eq("rohhd"), eq("노학동"));
    }

    @Test
    void 영수금액이_없는_줄은_영수금액을_넘기지_않아_그대로_둔다() throws Exception {
        line.setReciptAmt(null);

        run(row("주문번호", 1001, "판매액", 1200));

        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(priceMapper).updateLine(cap.capture());
        assertNull(cap.getValue().get("reciptAmt"));
    }

    @Test
    void 오류와_변경없음_행은_고치지_않고_이력에는_모두_남긴다() throws Exception {
        OrderPriceResult r = run(row("주문번호", 1001, "판매액", 1200), row("주문번호", 9999, "판매액", 1), row("주문번호", 1001, "순번", 2, "판매액", 1));
        // 둘째·셋째 행은 줄이 없어 오류. 변경없음은 따로 확인
        assertEquals(1, r.getAppliedCount());
        assertEquals(2, r.getErrorCount());
        verify(priceMapper, times(1)).updateLine(anyMap());
        ArgumentCaptor<OrderExcelUploadDetail> dc = ArgumentCaptor.forClass(OrderExcelUploadDetail.class);
        verify(historyMapper, times(3)).insertUploadDetail(dc.capture());
        assertEquals(List.of("성공", "오류", "오류"), dc.getAllValues().stream().map(OrderExcelUploadDetail::getResultType).toList());

        reset(priceMapper, historyMapper);
        when(priceMapper.selectGuard(anyInt(), anyInt())).thenReturn(guard);
        doAnswer(inv -> { ((OrderExcelUpload) inv.getArgument(0)).setUploadId(778); return 1; }).when(historyMapper).insertUpload(any());
        OrderPriceResult same = run(row("주문번호", 1001), row("주문번호", 1001, "순번", 1, "판매액", 1200));
        assertEquals(1, same.getSameCount());   // 둘째 행은 같은 키가 두 번째라 오류
        verify(priceMapper, never()).updateLine(anyMap());
    }

    @Test
    void 바뀌는_값이_없으면_등록하지_않고_이력도_남기지_않는다() throws Exception {
        OrderPriceResult r = run(row("주문번호", 1001));

        assertFalse(r.isImported());
        assertNull(r.getUploadId());
        verify(historyMapper, never()).insertUpload(any());
        verifyNoInteractions(lineLock);
    }

    @Test
    void 잠근_뒤_최신_상태가_고칠_수_없으면_그_행만_오류가_된다() throws Exception {
        OrderProd second = line(1002, 1, "지불완료");
        when(orderProdMapper.selectLineRaw(anyMap())).thenAnswer(inv -> {
            Map<String, Object> k = inv.getArgument(0);
            return Integer.valueOf(1001).equals(k.get("orderNo")) ? line : second;
        });
        OrderProd locked1001 = line(1001, 1, "환불완료");   // 검사 뒤 환불이 확정됐다
        when(lineLock.lockLine(1001, 1)).thenReturn(locked1001);
        when(lineLock.lockLine(1002, 1)).thenReturn(second);

        OrderPriceResult r = run(row("주문번호", 1001, "판매액", 1200), row("주문번호", 1002, "판매액", 1200));

        assertEquals(OrderPriceRow.ERROR, r.getRows().get(0).getStatus());
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("환불완료"));
        assertTrue(r.getRows().get(1).isApplied());
        assertEquals(1, r.getAppliedCount());
        verify(priceMapper, times(1)).updateLine(anyMap());
    }

    @Test
    void 줄_상태가_그_사이_바뀌어_update_가_0행이면_오류다() throws Exception {
        when(priceMapper.updateLine(anyMap())).thenReturn(0);

        OrderPriceResult r = run(row("주문번호", 1001, "판매액", 1200));

        assertEquals(OrderPriceRow.ERROR, r.getRows().get(0).getStatus());
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("다시 검사"));
        assertEquals(0, r.getAppliedCount());
        verify(priceMapper, never()).updateShipCopy(anyMap());
    }

    @Test
    void 한_행의_예외가_다른_행에_영향을_주지_않는다() throws Exception {
        OrderProd second = line(1002, 1, "지불완료");
        when(orderProdMapper.selectLineRaw(anyMap())).thenAnswer(inv -> {
            Map<String, Object> k = inv.getArgument(0);
            return Integer.valueOf(1001).equals(k.get("orderNo")) ? line : second;
        });
        when(lineLock.lockLine(1002, 1)).thenReturn(second);
        when(priceMapper.updateLine(anyMap())).thenThrow(new IllegalStateException("db down")).thenReturn(1);

        OrderPriceResult r = run(row("주문번호", 1001, "판매액", 1200), row("주문번호", 1002, "판매액", 1200));

        assertEquals(OrderPriceRow.ERROR, r.getRows().get(0).getStatus());
        assertTrue(r.getRows().get(0).getErrors().get(0).contains("수정 중 오류"));
        assertTrue(r.getRows().get(1).isApplied());
        assertEquals(1, r.getAppliedCount());
    }

    @Test
    void 줄을_못_찾는_상태면_오류다() throws Exception {
        when(lineLock.lockLine(1001, 1)).thenReturn(null);

        OrderPriceResult r = run(row("주문번호", 1001, "판매액", 1200));

        assertTrue(r.getRows().get(0).getErrors().get(0).contains("찾을 수 없습니다"));
    }
}
