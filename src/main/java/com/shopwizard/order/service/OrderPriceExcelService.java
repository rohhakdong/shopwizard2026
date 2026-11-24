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
import com.shopwizard.order.model.OrderPriceRow.Input;
import com.shopwizard.order.model.OrderProd;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * 주문 엑셀 가격수정("엑셀가격수정") — 주문번호+순번으로 기존 주문 줄을 찾아 판매액·채널공급가·원가·판촉비·배송형태·배송비를 고친다.
 * 레거시 {@code OrderServiceImpl#runUpdatePriceFromExcel} 을 이식하되 수량·상품은 바꾸지 않는다(엑셀의 수량이 다르면 오류).
 *
 * <ul>
 *   <li>[검사]({@link #preview})는 행마다 <b>변경 / 변경 없음 / 오류</b>와 바뀌는 값(전→후)을 보여 주고, [등록]({@link #importFile})은
 *       다시 검사한 뒤 변경 행만 행마다 한 트랜잭션으로 고친다. 칸을 비우면 현재 값을 유지한다.</li>
 *   <li>판매액·공급가·원가는 <b>1개 단가</b>로 보고 수량을 곱해 줄 합계로 저장한다. 판매액은 옵션가를 포함한 값이라 줄의 옵션가(NvPrice)를
 *       뺀 만큼이 SalePrice 다(레거시와 같음). 마진은 줄 합계로 다시 계산하고, 부가세액은 <b>새</b> 가격으로 계산한다.</li>
 *   <li>정산마감된 줄(정산자료에 마감 표시가 있는 줄)은 고칠 수 없다. 환불완료·주문접수·결제오류 줄과 <b>PG 결제 주문</b>(이미 카드·계좌로
 *       결제된 금액)도 거절한다. 그 밖의 상태는 마감 전이면 고치고, 있으면 출고 사본·미마감 정산자료·반품 사본도 같이 맞춘다.</li>
 *   <li>업로드마다 {@code tOrdExcelUpload}(FileLoc 가 "[가격수정] …")과 행별 {@code tOrdExcelUploadDetail} 이력을 남긴다.</li>
 * </ul>
 */
@Service
public class OrderPriceExcelService {

    private static final Logger log = LoggerFactory.getLogger(OrderPriceExcelService.class);

    public static final int MAX_ROWS = 300;
    static final String HISTORY_PREFIX = "[가격수정] ";
    /** 샵링커 / 엑셀 / 수기 — PG 결제가 아닌 주문만 가격을 고칠 수 있다. */
    static final Set<String> EDITABLE_PAY_TYPES = Set.of("D00000000000", "E00000000000", "M00000000000");
    /** 가격을 고칠 수 있는 줄 상태(마감 전). 그 밖(주문접수·결제오류·환불완료 등)은 거절한다. */
    static final Set<String> EDITABLE_STATES = Set.of("지불완료", "출고지시", "발주확인", "배송시작", "배송완료", "주문취소", "반품요청", "반품완료");
    private static final Object IMPORT_LOCK = new Object();

    /** 양식 열(레거시 priceExcel.xls 헤더 이름 그대로). {열 이름, 구분, 설명}. 두 번째 시트 "작성안내"에도 쓴다. */
    static final String[][] COLUMNS = {
        {"주문번호", "필수", "주문관리 목록의 주문번호."},
        {"순번", "선택", "주문 줄 순번(주문관리 목록의 순번). 비우면 1."},
        {"상점상품코드", "참고", "읽기만 합니다. 주문 줄의 상점상품코드와 다르면 경고만 표시합니다(상품은 바꾸지 않습니다)."},
        {"상품명", "무시", "참고용입니다."},
        {"상품옵션", "무시", "참고용입니다."},
        {"수량", "참고", "주문 수량과 같아야 합니다. 다르면 오류입니다(수량은 이 기능으로 바꿀 수 없습니다). 비우면 확인하지 않습니다."},
        {"판매액", "선택", "수량을 곱하기 전 1개 가격(옵션가 포함, 0 이상 정수). 비우면 현재 값 유지."},
        {"채널공급가", "선택", "수량을 곱하기 전 1개 가격(0 이상 정수). 비우면 현재 값 유지."},
        {"원가", "선택", "수량을 곱하기 전 1개 가격(0 이상 정수). 비우면 현재 값 유지. 출고지시 전(지불완료) 줄의 원가는 출고지시 때 발주단가 규칙으로 다시 계산되어 덮어쓰일 수 있습니다."},
        {"판촉비", "선택", "줄 금액 그대로(수량을 곱하지 않습니다), 0 이상 정수. 비우면 현재 값 유지."},
        {"배송형태", "선택", "선불 / 착불 / 무료 ('선결제', '배송비:유료'는 선불, 그 밖의 값은 무료). 비우면 현재 값 유지."},
        {"배송비", "선택", "줄 금액 그대로, 0 이상 정수. 비우면 현재 값 유지."},
    };

    private static final String H_ORDER_NO = "주문번호", H_PROD_NO = "순번", H_SHOP_PROD_CODE = "상점상품코드", H_QTY = "수량",
            H_SALE = "판매액", H_SUPPLY = "채널공급가", H_BUY = "원가", H_PROMOT = "판촉비", H_DELI_TYPE = "배송형태", H_DELI_AMT = "배송비";

    private final OrderPriceMapper priceMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderExcelMapper historyMapper;
    private final OrderLineLock lineLock;
    private final DataScopeGuard scopeGuard;
    private final TransactionTemplate tx;

    public OrderPriceExcelService(OrderPriceMapper priceMapper, OrderProdMapper orderProdMapper, OrderExcelMapper historyMapper,
                                  OrderLineLock lineLock, DataScopeGuard scopeGuard, PlatformTransactionManager txManager) {
        this.priceMapper = priceMapper;
        this.orderProdMapper = orderProdMapper;
        this.historyMapper = historyMapper;
        this.lineLock = lineLock;
        this.scopeGuard = scopeGuard;
        this.tx = new TransactionTemplate(txManager);
    }

    // ── 공개 API ────────────────────────────────────────────

    /** 검사만 한다. DB 는 바꾸지 않는다. */
    public OrderPriceResult preview(InputStream in, HttpServletRequest request) {
        return check(in, request);
    }

    /** 다시 검사한 뒤 바뀌는 값이 있는 정상 행만 고친다(행마다 한 트랜잭션). 업로드 전체는 직렬화한다. */
    public OrderPriceResult importFile(InputStream in, String fileName, HttpServletRequest request) {
        synchronized (IMPORT_LOCK) {
            OrderPriceResult result = check(in, request);
            if (result.getFileError() != null || result.getOkCount() == 0) return result;

            AuthSession.Actor actor = AuthSession.actor(request);
            String registId = actor == null || actor.loginId() == null ? "SYSTEM" : actor.loginId();
            String registName = actor == null || actor.name() == null ? registId : actor.name();

            OrderExcelUpload upload = new OrderExcelUpload();
            upload.setFileLoc(cut(HISTORY_PREFIX + (fileName == null ? "" : fileName), 500));
            upload.setRegistId(registId);
            upload.setRegistName(registName);
            tx.executeWithoutResult(s -> historyMapper.insertUpload(upload));
            int uploadId = upload.getUploadId();

            for (OrderPriceRow row : result.getRows()) {
                if (OrderPriceRow.OK.equals(row.getStatus())) applyRow(row, registId, registName);
                writeDetail(uploadId, row, registId, registName);
            }
            result.summarize();
            result.setImported(true);
            result.setUploadId(uploadId);
            String message = "처리를 완료하였습니다. 변경없음 - " + result.getSameCount() + "건, 오류 - " + result.getErrorCount()
                    + "건, 성공 - " + result.getAppliedCount() + "건";
            result.setMessage(message);
            tx.executeWithoutResult(s -> historyMapper.finishUpload(uploadId, "성공", message, registId, registName));
            return result;
        }
    }

    /** 빈 양식(.xls) — 첫 시트는 헤더와 샘플 1행, 두 번째 시트는 작성 안내. */
    public byte[] buildTemplate() {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(COLUMNS[i][0]);
                c.setCellStyle(head);
                sheet.setColumnWidth(i, (COLUMNS[i][0].length() < 5 ? 12 : 16) * 256);
            }
            sheet.createFreezePane(0, 1);
            Object[] sample = {2415370, 1, "sample_0001", "샘플 상품", "선택사항없음", 1, 10000, 9000, 7000, 0, "무료", 0};
            Row s = sheet.createRow(1);
            for (int i = 0; i < sample.length; i++) {
                if (sample[i] instanceof Number n) s.createCell(i).setCellValue(n.doubleValue());
                else s.createCell(i).setCellValue(String.valueOf(sample[i]));
            }

            Sheet guide = wb.createSheet("작성안내");
            Row gh = guide.createRow(0);
            String[] titles = {"열 이름", "구분", "설명"};
            for (int i = 0; i < titles.length; i++) {
                Cell c = gh.createCell(i);
                c.setCellValue(titles[i]);
                c.setCellStyle(head);
            }
            for (int c = 0; c < COLUMNS.length; c++) {
                Row row = guide.createRow(c + 1);
                for (int i = 0; i < 3; i++) row.createCell(i).setCellValue(COLUMNS[c][i]);
            }
            int n = COLUMNS.length + 2;
            String[] lines = {
                "※ 첫 시트(Sheet1)의 1행은 헤더, 2행부터 수정할 주문 줄입니다(샘플 행은 지우고 쓰세요). 열 순서는 바뀌어도 되고 헤더 이름으로 찾습니다.",
                "※ 주문번호+순번으로 기존 주문 줄을 찾아 가격만 고칩니다. 수량·상품은 바꾸지 않습니다. 한 번에 최대 " + MAX_ROWS + "행까지 올릴 수 있습니다.",
                "※ 판매액·채널공급가·원가는 수량을 곱하기 전 1개 가격입니다. 저장할 때 수량을 곱하고 마진을 다시 계산합니다.",
                "※ [검사]에서 행마다 변경 / 변경 없음 / 오류와 바뀌는 값이 표시되고, [등록]하면 변경 행만 고쳐집니다.",
                "※ 정산마감된 줄, 환불완료 줄, 쇼핑몰(PG) 결제 주문은 고칠 수 없습니다. 출고지시 이후 줄은 출고 사본과 마감 전 정산자료도 함께 바뀝니다.",
            };
            for (String line : lines) guide.createRow(n++).createCell(0).setCellValue(line);
            guide.setColumnWidth(0, 14 * 256);
            guide.setColumnWidth(1, 8 * 256);
            guide.setColumnWidth(2, 110 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 양식을 만들 수 없습니다.", e);
        }
    }

    // ── 수정 ────────────────────────────────────────────────

    /** 한 줄을 고친다. 거절·실패는 그 행의 오류로만 남고 다른 행에 영향을 주지 않는다. */
    private void applyRow(OrderPriceRow row, String registId, String registName) {
        try {
            List<String> changes = tx.execute(s -> updateLine(row, registId, registName));
            if (changes != null) row.setApplied(true);
        } catch (RowRejected e) {
            row.error(e.getMessage());
        } catch (Exception e) {
            log.warn("주문 가격수정 행 {} 실패(주문 {}-{})", row.getRowNo(), row.getOrderNo(), row.getOrderProdNo(), e);
            row.error("수정 중 오류가 발생했습니다: " + cut(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 200));
        }
    }

    /** 잠금 → 최신 값으로 다시 검사·계산 → 줄과 사본·정산자료를 한 번에 갱신. 트랜잭션 안에서 호출한다. */
    private List<String> updateLine(OrderPriceRow row, String registId, String registName) {
        int orderNo = row.getOrderNo();
        int orderProdNo = row.getOrderProdNo();
        OrderProd line = lineLock.lockLine(orderNo, orderProdNo);
        if (line == null) throw new RowRejected("주문 줄을 찾을 수 없습니다.");
        List<String> refusals = refusals(line, priceMapper.selectGuard(orderNo, orderProdNo));
        if (!refusals.isEmpty()) throw new RowRejected(refusals.get(0));
        Plan plan = plan(line, row.getInput());
        if (!plan.errors.isEmpty()) throw new RowRejected(plan.errors.get(0));
        if (plan.changes.isEmpty()) return null;   // 그 사이 다른 사람이 같은 값으로 바꿨다 — 할 일 없음

        Map<String, Object> p = new HashMap<>();
        p.put("orderNo", orderNo);
        p.put("orderProdNo", orderProdNo);
        p.put("expectedState", line.getOrderState());
        p.put("prodQty", line.getProdQty());
        p.put("salePrice", plan.sale);
        p.put("supplyPrice", plan.supply);
        p.put("buyPrice", plan.buy);
        p.put("vatAmt", plan.vatAmt);
        p.put("prodMargin", plan.prodMargin);
        p.put("chnlMargin", plan.chnlMargin);
        p.put("netMargin", plan.netMargin);
        p.put("reciptAmt", plan.reciptAmt);
        p.put("deliFeeType", plan.deliFeeType);
        p.put("deliFeeAmt", plan.deliFeeAmt);
        p.put("promotFeeAmt", plan.promotFeeAmt);
        p.put("changeId", registId);
        p.put("changeName", registName);
        if (priceMapper.updateLine(p) != 1) throw new RowRejected("주문 줄의 상태가 그 사이 바뀌었습니다. 다시 검사해 주세요.");
        // 사본·정산자료는 있을 때만 바뀐다(조건에 맞는 행이 없으면 0행).
        priceMapper.updateShipCopy(p);
        priceMapper.updateReturnCopy(p);
        priceMapper.updateOpenShipDetail(p);
        priceMapper.updateOpenReturnDetail(p);
        return plan.changes;
    }

    private void writeDetail(int uploadId, OrderPriceRow row, String registId, String registName) {
        OrderExcelUploadDetail d = new OrderExcelUploadDetail();
        d.setUploadId(uploadId);
        d.setUploadDetailNo(row.getRowNo());
        d.setExcelOrderNo(cut(row.getKey(), 50));
        switch (row.getStatus()) {
            case OrderPriceRow.ERROR -> {
                d.setResultType("오류");
                d.setResultMessag(cut(String.join(" / ", row.getErrors()), 500));
            }
            case OrderPriceRow.SAME -> {
                d.setResultType("변경없음");
                d.setResultMessag("바뀌는 값이 없어 건너뜁니다.");
            }
            default -> {
                d.setResultType("성공");
                d.setResultMessag(cut(String.join(", ", row.getChanges()), 500));
            }
        }
        d.setRegistId(registId);
        d.setRegistName(registName);
        tx.executeWithoutResult(s -> historyMapper.insertUploadDetail(d));
    }

    // ── 검사 ────────────────────────────────────────────────

    private OrderPriceResult check(InputStream in, HttpServletRequest request) {
        List<RawRow> raw;
        try {
            raw = parse(in);
        } catch (ExcelFileException e) {
            return OrderPriceResult.ofFileError(e.getMessage());
        }
        Set<String> seen = new HashSet<>();
        OrderPriceResult result = new OrderPriceResult();
        for (RawRow r : raw) result.getRows().add(validate(r, request, seen));
        result.summarize();
        return result;
    }

    private OrderPriceRow validate(RawRow raw, HttpServletRequest request, Set<String> seen) {
        OrderPriceRow row = new OrderPriceRow();
        row.setRowNo(raw.rowNo);

        Integer orderNo = intCell(row, raw, H_ORDER_NO, true, 1, Integer.MAX_VALUE);
        Integer orderProdNo = raw.get(H_PROD_NO).isEmpty() ? Integer.valueOf(1) : intCell(row, raw, H_PROD_NO, false, 1, Integer.MAX_VALUE);
        row.setOrderNo(orderNo);
        row.setOrderProdNo(orderProdNo);

        Input in = new Input();
        in.setSaleUnit(intCell(row, raw, H_SALE, false, 0, Integer.MAX_VALUE));
        in.setSupplyUnit(intCell(row, raw, H_SUPPLY, false, 0, Integer.MAX_VALUE));
        in.setBuyUnit(intCell(row, raw, H_BUY, false, 0, Integer.MAX_VALUE));
        in.setPromotFeeAmt(intCell(row, raw, H_PROMOT, false, 0, Integer.MAX_VALUE));
        in.setDeliFeeAmt(intCell(row, raw, H_DELI_AMT, false, 0, Integer.MAX_VALUE));
        String deliRaw = raw.get(H_DELI_TYPE);
        in.setDeliFeeType(deliRaw.isEmpty() ? null : OrderExcelService.deliFeeType(deliRaw));
        Integer excelQty = intCell(row, raw, H_QTY, false, 0, Integer.MAX_VALUE);

        if (orderNo == null || orderProdNo == null) return row;   // 키가 틀리면 줄을 찾을 수 없다
        if (!seen.add(orderNo + "-" + orderProdNo)) {
            row.error("파일 안에서 주문 " + orderNo + "-" + orderProdNo + "이(가) 두 번 나옵니다. 앞의 행만 처리됩니다.");
            return row;
        }
        try {
            scopeGuard.requireLine(request, orderNo, orderProdNo);
        } catch (ResponseStatusException e) {
            row.error("주문 줄을 찾을 수 없거나 다른 업체(채널)의 주문입니다. 주문번호와 순번을 확인하세요.");   // 범위 밖 줄은 없는 줄과 똑같이 보인다(존재 여부를 알려주지 않는다)
            return row;
        }
        Map<String, Object> key = new HashMap<>();
        key.put("orderNo", orderNo);
        key.put("orderProdNo", orderProdNo);
        OrderProd line = orderProdMapper.selectLineRaw(key);
        if (line == null) {
            row.error("주문 줄을 찾을 수 없습니다. 주문번호와 순번을 확인하세요.");
            return row;
        }
        row.setProdName(line.getProdName());
        row.setOrderState(line.getOrderState());

        if (excelQty != null && !excelQty.equals(line.getProdQty())) {
            row.error("수량이 주문과 다릅니다(주문 " + line.getProdQty() + "개, 엑셀 " + excelQty + "개). 수량은 이 기능으로 바꿀 수 없습니다.");
        }
        String excelShopProd = raw.get(H_SHOP_PROD_CODE);
        if (!excelShopProd.isEmpty() && line.getShopProdCode() != null && !excelShopProd.equals(line.getShopProdCode())) {
            row.warn("상점상품코드가 주문 줄(" + line.getShopProdCode() + ")과 다릅니다. 상품은 바꾸지 않습니다.");
        }

        Map<String, Object> guard = priceMapper.selectGuard(orderNo, orderProdNo);
        List<String> refusals = refusals(line, guard);
        refusals.forEach(row::error);
        if (!row.getErrors().isEmpty()) return row;

        Plan plan = plan(line, in);
        plan.errors.forEach(row::error);
        if (!row.getErrors().isEmpty()) return row;
        row.getChanges().addAll(plan.changes);
        row.setInput(in);

        if (!plan.changes.isEmpty()) {
            if (count(guard, "shipCopyCnt") > 0) row.warn("출고 사본(" + count(guard, "shipCopyCnt") + "건)도 함께 바뀝니다.");
            if (count(guard, "openCnt") > 0) row.warn("마감 전 정산자료(" + count(guard, "openCnt") + "건)도 함께 바뀝니다. 정산 보정으로 고친 값은 덮어써집니다.");
            if (count(guard, "returnCopyCnt") > 0) row.warn("반품 사본(" + count(guard, "returnCopyCnt") + "건)도 함께 바뀝니다.");
            if ("지불완료".equals(line.getOrderState()) && in.getBuyUnit() != null && plan.buy != nz(line.getBuyPrice())) {
                row.warn("원가는 출고지시 때 발주단가 규칙으로 다시 계산되어 덮어쓰일 수 있습니다.");
            }
        }
        return row;
    }

    /** 이 줄을 고칠 수 없는 이유들(비어 있으면 고칠 수 있다). */
    static List<String> refusals(OrderProd line, Map<String, Object> guard) {
        List<String> r = new ArrayList<>();
        String state = line.getOrderState();
        if (state == null || !EDITABLE_STATES.contains(state)) {
            r.add("'" + nzs(state) + "' 상태의 주문 줄은 가격을 수정할 수 없습니다.");
        }
        if (count(guard, "closedCnt") > 0) r.add("정산마감된 줄은 가격을 수정할 수 없습니다.");
        Object payType = guard == null ? null : guard.get("payType");
        if (payType == null || !EDITABLE_PAY_TYPES.contains(String.valueOf(payType))) {
            r.add("쇼핑몰(PG) 결제 주문은 이미 결제된 금액이라 가격을 수정할 수 없습니다.");
        }
        return r;
    }

    private static int count(Map<String, Object> guard, String key) {
        Object v = guard == null ? null : guard.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    /** 엑셀 입력으로 바뀔 값. {@code changes} 가 비어 있으면 바뀌는 값이 없다. */
    static final class Plan {
        int sale, supply, buy, vatAmt, prodMargin, chnlMargin, netMargin, deliFeeAmt, promotFeeAmt;
        Integer reciptAmt;
        String deliFeeType;
        final List<String> changes = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
    }

    /**
     * 줄의 현재 값과 엑셀 입력으로 새 값을 계산한다. 금액은 줄 합계 기준: 판매액(옵션가 포함 1개 단가)×수량 에서 옵션가(NvPrice)를 뺀 만큼이
     * SalePrice, 공급가·원가는 단가×수량. 마진은 {@code ProdMatchService.backfillLine}/출고지시와 같은 식, 부가세액은 새 SalePrice 기준.
     */
    static Plan plan(OrderProd line, Input in) {
        Plan p = new Plan();
        int qty = nz(line.getProdQty());
        long nv = nz(line.getNvPrice());
        int curSale = nz(line.getSalePrice()), curSupply = nz(line.getSupplyPrice()), curBuy = nz(line.getBuyPrice());
        p.sale = curSale; p.supply = curSupply; p.buy = curBuy;
        p.promotFeeAmt = nz(line.getPromotFeeAmt());
        p.deliFeeAmt = nz(line.getDeliFeeAmt());
        p.deliFeeType = line.getDeliFeeType();

        if (in.getSaleUnit() != null) {
            long total = (long) in.getSaleUnit() * qty - nv;
            if (total < 0) p.errors.add("판매액이 옵션가보다 작습니다(옵션가 합계 " + won(nv) + "원).");
            else if (total > Integer.MAX_VALUE) p.errors.add("판매액×수량 금액이 너무 큽니다.");
            else p.sale = (int) total;
        }
        if (in.getSupplyUnit() != null) {
            long total = (long) in.getSupplyUnit() * qty;
            if (total > Integer.MAX_VALUE) p.errors.add("채널공급가×수량 금액이 너무 큽니다.");
            else p.supply = (int) total;
        }
        if (in.getBuyUnit() != null) {
            long total = (long) in.getBuyUnit() * qty;
            if (total > Integer.MAX_VALUE) p.errors.add("원가×수량 금액이 너무 큽니다.");
            else p.buy = (int) total;
        }
        if (in.getPromotFeeAmt() != null) p.promotFeeAmt = in.getPromotFeeAmt();
        if (in.getDeliFeeAmt() != null) p.deliFeeAmt = in.getDeliFeeAmt();
        if (in.getDeliFeeType() != null) p.deliFeeType = in.getDeliFeeType();
        if (!p.errors.isEmpty()) return p;

        boolean saleChanged = p.sale != curSale;
        boolean moneyChanged = saleChanged || p.supply != curSupply || p.buy != curBuy || p.promotFeeAmt != nz(line.getPromotFeeAmt());
        int vatRate = nz(line.getVatRate());
        p.vatAmt = saleChanged ? (vatRate > 0 ? (int) ((long) p.sale * 100 / (100 + vatRate)) : 0) : nz(line.getVatAmt());
        long real = (long) p.sale + nv;
        if (moneyChanged) {
            p.prodMargin = (int) (real - p.buy);
            p.chnlMargin = (int) (real - p.supply);
            p.netMargin = p.supply - p.buy - p.promotFeeAmt;
        } else {
            p.prodMargin = nz(line.getProdMargin());
            p.chnlMargin = nz(line.getChnlMargin());
            p.netMargin = nz(line.getNetMargin());
        }
        // 엑셀·수기 줄처럼 영수금액(ReciptAmt)이 있는 줄만 판매액에 맞춘다(PG 결제 줄은 위에서 이미 거절).
        if (saleChanged && line.getReciptAmt() != null && line.getReciptAmt() > 0) p.reciptAmt = (int) real;

        if (saleChanged) p.changes.add("판매 줄합계 " + won(nv + curSale) + "→" + won(real));
        if (p.supply != curSupply) p.changes.add("공급 " + won(curSupply) + "→" + won(p.supply));
        if (p.buy != curBuy) p.changes.add("원가 " + won(curBuy) + "→" + won(p.buy));
        if (p.promotFeeAmt != nz(line.getPromotFeeAmt())) p.changes.add("판촉비 " + won(nz(line.getPromotFeeAmt())) + "→" + won(p.promotFeeAmt));
        if (p.deliFeeAmt != nz(line.getDeliFeeAmt())) p.changes.add("배송비 " + won(nz(line.getDeliFeeAmt())) + "→" + won(p.deliFeeAmt));
        if (!Objects.equals(p.deliFeeType, line.getDeliFeeType())) p.changes.add("배송형태 " + nzs(line.getDeliFeeType()) + "→" + nzs(p.deliFeeType));
        return p;
    }

    // ── 셀 해석 ─────────────────────────────────────────────

    /** 정수(쉼표·소수점 .0·끝의 '원' 허용). 빈 칸은 null(=현재 값 유지), {@code required} 면 필수 오류도 남긴다. */
    private static Integer intCell(OrderPriceRow row, RawRow raw, String header, boolean required, int min, int max) {
        String s = raw.get(header);
        if (s.isEmpty()) {
            if (required) row.error(header + "을(를) 입력하세요.");
            return null;
        }
        String t = s.replace(",", "").replace(" ", "");
        if (t.endsWith("원")) t = t.substring(0, t.length() - 1);
        try {
            BigDecimal d = new BigDecimal(t);
            if (d.stripTrailingZeros().scale() > 0) throw new NumberFormatException();
            long v = d.longValueExact();
            if (v < min || v > max) {
                row.error(header + " 값이 범위를 벗어났습니다: " + s + " (" + min + " 이상)");
                return null;
            }
            return (int) v;
        } catch (NumberFormatException | ArithmeticException e) {
            row.error(header + "은(는) " + (min >= 1 ? "1 이상" : "0 이상") + " 정수여야 합니다: " + s);
            return null;
        }
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    private static String nzs(String s) { return s == null ? "" : s; }

    private static String won(long n) { return String.format("%,d", n); }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String squash(String s) { return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT); }

    // ── 엑셀 읽기 ───────────────────────────────────────────

    /** 파일에서 읽은 한 행 — 헤더 이름(공백 제거)으로 값을 찾는다. */
    static final class RawRow {
        final int rowNo;
        final Map<String, String> values;
        RawRow(int rowNo, Map<String, String> values) { this.rowNo = rowNo; this.values = values; }
        String get(String header) { return values.getOrDefault(squash(header), ""); }
    }

    static final class ExcelFileException extends RuntimeException {
        ExcelFileException(String message) { super(message); }
    }

    /** 처리 중 이 줄을 고칠 수 없다고 판단했을 때(트랜잭션을 되돌리고 그 행의 오류 메시지로 쓴다). */
    static final class RowRejected extends RuntimeException {
        RowRejected(String message) { super(message); }
    }

    List<RawRow> parse(InputStream in) {
        try (Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0) throw new ExcelFileException("시트가 없는 엑셀 파일입니다.");
            Sheet sheet = wb.getSheetAt(0);
            Row head = sheet.getRow(sheet.getFirstRowNum());
            if (head == null) throw new ExcelFileException("헤더 행이 없습니다. 양식을 다시 받아 작성해 주세요.");

            DataFormatter fmt = new DataFormatter();
            Map<Integer, String> colName = new LinkedHashMap<>();
            for (Cell c : head) {
                String h = squash(fmt.formatCellValue(c));
                if (!h.isEmpty()) colName.put(c.getColumnIndex(), h);
            }
            List<String> missing = new ArrayList<>();
            for (String[] col : COLUMNS) {
                if ("필수".equals(col[1]) && !colName.containsValue(squash(col[0]))) missing.add(col[0]);
            }
            if (!missing.isEmpty()) {
                throw new ExcelFileException("필수 열을 찾을 수 없습니다: " + String.join(", ", missing)
                        + ". 첫 시트의 1행이 양식의 헤더인지 확인해 주세요.");
            }

            List<RawRow> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                boolean any = false;
                for (Map.Entry<Integer, String> e : colName.entrySet()) {
                    String text = OrderExcelService.cellText(row.getCell(e.getKey()));
                    if (!text.isEmpty()) any = true;
                    values.put(e.getValue(), text);
                }
                if (!any) continue;   // 완전히 빈 행은 건너뛴다
                if (rows.size() >= MAX_ROWS) throw new ExcelFileException("한 번에 " + MAX_ROWS + "행까지만 올릴 수 있습니다. 나누어 올려 주세요.");
                rows.add(new RawRow(r + 1, values));
            }
            if (rows.isEmpty()) throw new ExcelFileException("수정할 주문 행이 없습니다(2행부터 데이터를 입력하세요).");
            return rows;
        } catch (ExcelFileException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelFileException("엑셀 파일(.xls, .xlsx)을 읽을 수 없습니다.");
        }
    }
}
