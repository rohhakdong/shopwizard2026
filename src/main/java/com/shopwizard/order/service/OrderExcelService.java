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
import com.shopwizard.order.model.OrderExcelRow.Parsed;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 주문 엑셀 업로드("주문엑셀등록") — 홈앤쇼핑·농협e쇼핑 같은 채널 주문을 예전 양식(orderExcel.xls, 28열)으로 한 번에 등록한다.
 * 레거시 {@code OrderServiceImpl#runInsertOrderFromExcel} 을 이식했다.
 *
 * <ul>
 *   <li>한 행 = 주문 1건(상품 1줄). 열은 순서가 아니라 <b>헤더 이름</b>으로 찾는다.</li>
 *   <li>{@link #preview}는 검사만 하고, {@link #importFile}은 다시 검사한 뒤 <b>정상 행만</b> 행마다 한 트랜잭션으로 저장한다.
 *       오류·중복 행은 건너뛰므로 같은 파일을 다시 올려도 안전하다.</li>
 *   <li>중복 기준은 엑셀의 "주문번호"({@code tOrdOrderExcel} PK) — 레거시와 같다. 저장할 때 원본 28열도 이 테이블에 남긴다.</li>
 *   <li>가격 3열(공급·판매·매입)은 <b>1개 단가</b>로 보고 수량을 곱해 줄 합계로 저장한다(새 시스템의 주문 줄 금액 규칙).</li>
 *   <li>주문은 "지불완료", 결제 행은 "엑셀"(PayType {@value #PAY_TYPE_EXCEL}, PgCode {@value #PG_CODE_EXCEL}).
 *       상품은 상점상품코드 → 상품코드 → 상품명+옵션 규칙 순으로 매칭하고, 매칭이 안 되면 경고만 하고 저장한다.
 *       재고는 매칭된 상품만 샵링커 수집처럼 원장 우선으로 차감한다(이미 채널에서 결제된 주문이라 재고 부족으로 거절하지 않는다).</li>
 *   <li>업로드마다 {@code tOrdExcelUpload} 이력과 행별 {@code tOrdExcelUploadDetail} 을 남긴다(검증 오류 행 포함).</li>
 * </ul>
 */
@Service
public class OrderExcelService {

    private static final Logger log = LoggerFactory.getLogger(OrderExcelService.class);

    /** 행마다 DB 쓰기가 10여 건이라 원격 DB 에서도 한 요청이 오래 걸리지 않게 막는다(레거시 파일은 80행 안팎). */
    public static final int MAX_ROWS = 300;
    static final String PAY_TYPE_EXCEL = "E00000000000";   // wizardn.tCodConstrVal cPayType "엑셀"
    static final String PG_CODE_EXCEL = "EXCEL";
    static final String STATE_PAID = "지불완료";
    static final int MAX_QTY = 999;
    static final int OLD_ORDER_DAYS = 31;
    private static final Object IMPORT_LOCK = new Object();
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 양식 열(순서 = 예전 양식 그대로). {열 이름, 필수 여부, 설명}. 두 번째 시트 "작성안내"에도 쓴다. */
    static final String[][] COLUMNS = {
        {"주문번호", "필수", "엑셀 파일 안에서 주문마다 다른 번호(20자 이내). 이 번호가 이미 처리된 적이 있으면 '중복'으로 건너뜁니다."},
        {"채널명", "필수", "채널 관리에 등록된 채널 이름과 정확히 같아야 합니다."},
        {"채널주문번호", "필수", "채널(쇼핑몰)의 주문번호. 50자 이내."},
        {"주문자명", "필수", "50자 이내."},
        {"주문자 전화번호", "선택", ""},
        {"주문자 휴대번호", "선택", ""},
        {"수취인", "필수", "50자 이내."},
        {"수취인 전화번호", "선택*", "*수취인 전화번호·휴대번호 중 하나는 필요합니다."},
        {"수취인 휴대번호", "선택*", "*수취인 전화번호·휴대번호 중 하나는 필요합니다."},
        {"수취인 우편번호", "선택", ""},
        {"수취인 주소", "필수", "255자 이내."},
        {"배송 메모", "선택", "255자 이내."},
        {"상점 상품 코드", "선택", "상품 매칭에 쓰입니다. 판매 상품의 상점상품코드와 같으면 그 상품에 연결됩니다."},
        {"상품 코드", "선택", "10자리 상품코드. 상점 상품 코드로 못 찾을 때 씁니다."},
        {"상품 명칭", "필수", "255자 이내."},
        {"상점 명칭", "선택", "참고용입니다."},
        {"공급 가격", "필수", "수량을 곱하기 전 1개 가격(0 이상 정수). 채널 공급가이며 0이면 상품 마스터의 공급가를 씁니다."},
        {"판매 가격", "필수", "수량을 곱하기 전 1개 가격(0 이상 정수). 0원은 증정품용으로 허용하지만 경고가 표시됩니다."},
        {"매입 가격", "선택", "수량을 곱하기 전 1개 가격. 상품이 연결되면 상품 마스터의 매입가를 쓰고, 연결이 안 된 주문에만 이 값을 씁니다."},
        {"주문 수량", "필수", "1~" + MAX_QTY + " 정수."},
        {"선택 옵션", "선택", "옵션 이름(상품 매칭에도 쓰입니다). 512자 이내."},
        {"주문 일자", "필수", "yyyy-MM-dd. 오늘 이후는 올릴 수 없고, " + OLD_ORDER_DAYS + "일보다 오래된 날짜는 경고가 표시됩니다."},
        {"배송 일자", "무시", "예전 양식의 열입니다. 읽지 않습니다."},
        {"택배회사", "무시", "예전 양식의 열입니다. 읽지 않습니다."},
        {"송장", "무시", "예전 양식의 열입니다. 읽지 않습니다."},
        {"배송비 타입", "필수", "선불 / 착불 / 무료. ('선결제', '배송비:유료'는 선불로 처리, 그 밖의 값은 무료)"},
        {"배송비", "선택", "0 이상 정수. 비우면 0."},
        {"판촉비", "선택", "0 이상 정수. 비우면 0."},
    };

    private static final String H_ORDER_NO = "주문번호", H_CHNL_NAME = "채널명", H_CHNL_ORDER_NO = "채널주문번호",
            H_ORDER_NAME = "주문자명", H_ORDER_PHONE = "주문자 전화번호", H_ORDER_MOBILE = "주문자 휴대번호",
            H_RECVER_NAME = "수취인", H_RECVER_PHONE = "수취인 전화번호", H_RECVER_MOBILE = "수취인 휴대번호",
            H_RECVER_ZIP = "수취인 우편번호", H_RECVER_ADDR = "수취인 주소", H_DELI_MEMO = "배송 메모",
            H_SHOP_PROD_CODE = "상점 상품 코드", H_PROD_CODE = "상품 코드", H_PROD_NAME = "상품 명칭", H_SHOP_NAME = "상점 명칭",
            H_SUPPLY = "공급 가격", H_SALE = "판매 가격", H_BUY = "매입 가격", H_QTY = "주문 수량", H_OPTION = "선택 옵션",
            H_ORDER_DATE = "주문 일자", H_DELI_TYPE = "배송비 타입", H_DELI_AMT = "배송비", H_PROMOT = "판촉비";

    private final OrderExcelMapper mapper;
    private final OrderNoSeqMapper orderNoSeqMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final OrderPayService orderPayService;
    private final StockLedgerService stockLedgerService;
    private final ProdService prodService;   // com.shopwizard.product.service.ProdService (판매 상품)
    private final ProdCodeMatchMapper prodCodeMatchMapper;
    private final DataScopeGuard scopeGuard;
    private final TransactionTemplate tx;

    public OrderExcelService(OrderExcelMapper mapper, OrderNoSeqMapper orderNoSeqMapper, OrderService orderService,
                             OrderProdService orderProdService, OrderPayService orderPayService,
                             StockLedgerService stockLedgerService, ProdService prodService,
                             ProdCodeMatchMapper prodCodeMatchMapper, DataScopeGuard scopeGuard,
                             PlatformTransactionManager txManager) {
        this.mapper = mapper;
        this.orderNoSeqMapper = orderNoSeqMapper;
        this.orderService = orderService;
        this.orderProdService = orderProdService;
        this.orderPayService = orderPayService;
        this.stockLedgerService = stockLedgerService;
        this.prodService = prodService;
        this.prodCodeMatchMapper = prodCodeMatchMapper;
        this.scopeGuard = scopeGuard;
        this.tx = new TransactionTemplate(txManager);
    }

    // ── 공개 API ────────────────────────────────────────────

    /** 검사만 한다. DB 는 바꾸지 않는다. */
    public OrderExcelResult preview(InputStream in, HttpServletRequest request) {
        return check(in, request);
    }

    /**
     * 다시 검사한 뒤 정상 행만 저장한다(행마다 한 트랜잭션 — 한 행의 실패가 다른 행에 영향을 주지 않는다).
     * 같은 파일을 동시에 올려도 같은 주문번호가 두 번 들어가지 않도록 업로드 전체를 직렬화하고, 원본 테이블의 PK 가 마지막 방어선이다.
     */
    public OrderExcelResult importFile(InputStream in, String fileName, HttpServletRequest request) {
        synchronized (IMPORT_LOCK) {
            OrderExcelResult result = check(in, request);
            if (result.getFileError() != null || result.getOkCount() == 0) return result;

            AuthSession.Actor actor = AuthSession.actor(request);
            String registId = actor == null || actor.loginId() == null ? "SYSTEM" : actor.loginId();
            String registName = actor == null || actor.name() == null ? registId : actor.name();

            OrderExcelUpload upload = new OrderExcelUpload();
            upload.setFileLoc(cut(fileName, 500));
            upload.setRegistId(registId);
            upload.setRegistName(registName);
            tx.executeWithoutResult(s -> mapper.insertUpload(upload));
            int uploadId = upload.getUploadId();

            for (OrderExcelRow row : result.getRows()) {
                if (OrderExcelRow.OK.equals(row.getStatus())) saveRow(row, registId, registName);
                writeDetail(uploadId, row, registId, registName);
            }
            result.summarize();
            result.setImported(true);
            result.setUploadId(uploadId);
            String message = "처리를 완료하였습니다. 중복 - " + result.getDupCount() + "건, 오류 - " + result.getErrorCount()
                    + "건, 성공 - " + result.getCreatedCount() + "건";
            result.setMessage(message);
            tx.executeWithoutResult(s -> mapper.finishUpload(uploadId, "성공", message, registId, registName));
            return result;
        }
    }

    /** 빈 양식(.xls) — 첫 시트는 28열 헤더와 샘플 1행, 두 번째 시트는 작성 안내. */
    public byte[] buildTemplate() {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // 번호·전화·우편번호·코드는 텍스트 서식 — 앞자리 0 이 사라지지 않게
            CellStyle text = wb.createCellStyle();
            text.setDataFormat(wb.createDataFormat().getFormat("@"));
            Set<String> textCols = Set.of(H_ORDER_NO, H_CHNL_ORDER_NO, H_ORDER_PHONE, H_ORDER_MOBILE, H_RECVER_PHONE,
                    H_RECVER_MOBILE, H_RECVER_ZIP, H_SHOP_PROD_CODE, H_PROD_CODE);

            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(COLUMNS[i][0]);
                c.setCellStyle(head);
                sheet.setColumnWidth(i, (COLUMNS[i][0].length() < 6 ? 14 : 18) * 256);
                if (textCols.contains(COLUMNS[i][0])) sheet.setDefaultColumnStyle(i, text);
            }
            sheet.createFreezePane(0, 1);
            Map<String, Object> sample = new HashMap<>();
            sample.put(H_ORDER_NO, "2026010101"); sample.put(H_CHNL_NAME, "홈앤쇼핑"); sample.put(H_CHNL_ORDER_NO, "20260101123456-001");
            sample.put(H_ORDER_NAME, "홍길동"); sample.put(H_ORDER_MOBILE, "010-1234-5678");
            sample.put(H_RECVER_NAME, "김철수"); sample.put(H_RECVER_MOBILE, "010-8765-4321"); sample.put(H_RECVER_ZIP, "04524");
            sample.put(H_RECVER_ADDR, "서울특별시 중구 세종대로 110"); sample.put(H_DELI_MEMO, "문 앞에 놓아주세요");
            sample.put(H_SHOP_PROD_CODE, "sample_0001"); sample.put(H_PROD_NAME, "샘플 상품"); sample.put(H_SHOP_NAME, "샘플상점");
            sample.put(H_SUPPLY, 9000); sample.put(H_SALE, 10000); sample.put(H_BUY, 7000); sample.put(H_QTY, 1);
            sample.put(H_OPTION, "선택사항없음"); sample.put(H_ORDER_DATE, LocalDate.now().toString());
            sample.put(H_DELI_TYPE, "무료"); sample.put(H_DELI_AMT, 0); sample.put(H_PROMOT, 0);
            Row s = sheet.createRow(1);
            for (int i = 0; i < COLUMNS.length; i++) {
                Object v = sample.get(COLUMNS[i][0]);
                if (v == null) continue;
                if (v instanceof Number n) s.createCell(i).setCellValue(n.doubleValue());
                else s.createCell(i).setCellValue(String.valueOf(v));
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
                "※ 첫 시트(Sheet1)의 1행은 헤더, 2행부터 주문 데이터입니다(샘플 행은 지우고 쓰세요). 열 순서는 바뀌어도 되고 헤더 이름으로 찾습니다.",
                "※ 한 행이 주문 1건(상품 1줄)입니다. 한 번에 최대 " + MAX_ROWS + "행까지 올릴 수 있습니다.",
                "※ 공급·판매·매입 가격은 수량을 곱하기 전 1개 기준입니다. 저장할 때 수량을 곱합니다.",
                "※ [검사]에서 행마다 정상 / 중복 / 오류가 표시되고, [등록]하면 정상 행만 저장됩니다. 오류·중복 행은 건너뛰며, 고쳐서 다시 올려도 이미 등록된 주문번호는 중복으로 빠집니다.",
                "※ 등록된 주문은 '지불완료' 상태, 결제는 '엑셀'로 기록됩니다. 상품이 연결되지 않은 주문은 경고와 함께 저장되며 상품 매칭 화면에서 연결하세요.",
            };
            for (String line : lines) guide.createRow(n++).createCell(0).setCellValue(line);
            guide.setColumnWidth(0, 18 * 256);
            guide.setColumnWidth(1, 8 * 256);
            guide.setColumnWidth(2, 100 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 양식을 만들 수 없습니다.", e);
        }
    }

    // ── 업로드 이력 ─────────────────────────────────────────

    /** 상점·채널 제한 계정은 자기가 올린 이력만 본다(이력에는 채널이 없어 올린 사람으로 가른다). */
    public List<OrderExcelUpload> uploads(Map<String, String> params, HttpServletRequest request) {
        int size = clamp(parseInt(params.get("pPageSize"), 20), 1, 100);
        int offset = Math.max(0, parseInt(params.get("pPageOffset"), 0));
        return mapper.selectUploadList(blankToNull(params.get("pStartDate")), blankToNull(params.get("pEndDate")),
                ownerFilter(request), offset, size);
    }

    public int uploadCount(Map<String, String> params, HttpServletRequest request) {
        return mapper.selectUploadCount(blankToNull(params.get("pStartDate")), blankToNull(params.get("pEndDate")), ownerFilter(request));
    }

    public List<OrderExcelUploadDetail> uploadDetails(int uploadId, HttpServletRequest request) {
        OrderExcelUpload upload = mapper.selectUpload(uploadId);
        String owner = ownerFilter(request);
        if (upload == null || (owner != null && !owner.equals(upload.getRegistId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 업체(채널)의 데이터는 처리할 수 없습니다.");
        }
        return mapper.selectUploadDetails(uploadId);
    }

    private String ownerFilter(HttpServletRequest request) {
        if (!scopeGuard.scope(request).scoped()) return null;
        AuthSession.Actor actor = AuthSession.actor(request);
        return actor == null || actor.loginId() == null ? "~none~" : actor.loginId();
    }

    // ── 저장 ────────────────────────────────────────────────

    /** 정상 행 한 건을 저장한다. 실패해도 그 행만 오류/중복으로 표시하고 다음 행으로 넘어간다. */
    private void saveRow(OrderExcelRow row, String registId, String registName) {
        try {
            Integer orderNo = tx.execute(s -> writeOrder(row.getParsed(), registId, registName));
            row.setOrderNo(orderNo);
            row.setResultMessage("처리에 성공하였습니다.");
        } catch (DuplicateKeyException e) {
            // 검사 뒤에 같은 주문번호가 먼저 들어갔다 — 트랜잭션은 롤백됐으므로 이 행은 중복으로 본다.
            row.setDuplicate(true);
        } catch (Exception e) {
            log.warn("주문 엑셀 행 {} 저장 실패(주문번호 {})", row.getRowNo(), row.getExcelOrderNo(), e);
            row.error("저장 중 오류가 발생했습니다: " + cut(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 200));
        }
    }

    private void writeDetail(int uploadId, OrderExcelRow row, String registId, String registName) {
        OrderExcelUploadDetail d = new OrderExcelUploadDetail();
        d.setUploadId(uploadId);
        d.setUploadDetailNo(row.getRowNo());
        d.setExcelOrderNo(cut(row.getExcelOrderNo() == null ? "" : row.getExcelOrderNo(), 50));
        switch (row.getStatus()) {
            case OrderExcelRow.ERROR -> {
                d.setResultType("오류");
                d.setResultMessag(cut(String.join(" / ", row.getErrors()), 500));
            }
            case OrderExcelRow.DUP -> {
                d.setResultType("중복");
                d.setResultMessag("이미 처리된 건과 중복입니다.");
            }
            default -> {
                d.setResultType("성공");
                d.setResultMessag("처리에 성공하였습니다." + (row.getOrderNo() != null ? " 주문번호 " + row.getOrderNo() : ""));
            }
        }
        d.setRegistId(registId);
        d.setRegistName(registName);
        tx.executeWithoutResult(s -> mapper.insertUploadDetail(d));
    }

    /** 한 행 = 주문 헤더 + 결제 + 상품 줄 + (매칭된 상품이면) 재고 차감 + 원본 보관. 한 트랜잭션 안에서 호출한다. */
    private int writeOrder(Parsed p, String registId, String registName) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DATETIME_FMT);

        OrderNoSeq seq = new OrderNoSeq();
        orderNoSeqMapper.insert(seq);
        int orderNo = seq.getSeq();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustId(0);
        order.setLoginId("");
        order.setOrderName(p.getOrderName());
        order.setOrderEmail("");
        order.setOrderPhoneNo(p.getOrderPhoneNo());
        order.setOrderMobileNo(p.getOrderMobileNo());
        order.setOrderZipcode("");
        order.setOrderAddr1("");
        order.setOrderAddr2("");
        order.setRecverName(p.getRecverName());
        order.setRecverPhoneNo(p.getRecverPhoneNo());
        order.setRecverMobileNo(p.getRecverMobileNo());
        order.setRecverZipcode(p.getRecverZipcode());
        order.setRecverAddr1(p.getRecverAddr());
        order.setRecverAddr2("");   // NULL 이 아니라 '' — 출고지시가 CONCAT 으로 주소를 합칠 때 NULL 이 섞이면 전체가 NULL
        order.setMoneyUnit("KRW");
        order.setDeliMemo(p.getDeliMemo());
        order.setOrderState(STATE_PAID);
        order.setOrderReciptDate(p.getOrderDate().atStartOfDay().format(DATETIME_FMT));
        order.setPayCmpletDate(nowStr);
        order.setChnlCode(p.getChnlCode());
        order.setRegistId(registId);
        order.setRegistName(registName);
        orderService.insert(order);

        int qty = p.getQty();
        int sale = Math.multiplyExact(p.getSalePrice(), qty);
        int supply = Math.multiplyExact(p.getSupplyPrice(), qty);
        int buy = Math.multiplyExact(p.getBuyPrice(), qty);

        OrderProd op = new OrderProd();
        op.setOrderNo(orderNo);
        op.setOrderProdNo(1);
        op.setOrderState(STATE_PAID);
        op.setState(1);
        op.setBundleYn(0);
        op.setAccntId("0");
        op.setShopProdCode(p.getShopProdCode());
        op.setProdName(p.getProdName());
        op.setItemName(p.getOptionName());
        op.setProdQty(qty);
        // 주문 라인 금액은 모든 채널에서 "수량이 곱해진 라인 합계"다. 엑셀의 1개 단가에 수량을 곱한다.
        op.setListPrice(sale);
        op.setSalePrice(sale);
        op.setNvPrice(0);
        op.setReciptAmt(sale);
        op.setSupplyPrice(supply);
        op.setBuyPrice(buy);
        op.setVatRate(0);
        op.setVatAmt(0);
        op.setDeliFeeType(p.getDeliFeeType());
        op.setDeliFeeAmt(p.getDeliFeeAmt());
        op.setPromotFeeAmt(p.getPromotFeeAmt());
        op.setChnlOrderNo(p.getChnlOrderNo());
        op.setRegistId(registId);
        op.setRegistName(registName);
        op.setProdMargin(sale - buy);
        op.setChnlMargin(sale - supply);
        op.setNetMargin(supply - buy - p.getPromotFeeAmt());
        Prod matched = p.getMatched();
        if (matched != null) {
            // 공급사·상점·제조사·세율, 매입가(상품 마스터×수량)와 마진을 상품 기준으로 채운다. 엑셀 공급가가 0보다 크면 그 값을 유지한다.
            ProdMatchService.backfillLine(op, matched);
        }
        orderProdService.insert(op);

        // 재고 차감(매칭된 경우만) — 원장을 먼저 선점하고 선점했을 때만 차감한다. 실패는 삼키지 않아 이 행 전체가 롤백된다.
        if (matched != null && stockLedgerService.claimDeduction(orderNo, 1, matched.getProdCode(), qty, registId)) {
            Map<String, Object> dec = new HashMap<>();
            dec.put("prodCode", matched.getProdCode());
            dec.put("qty", qty);
            prodService.updateSupplyQty(dec);
        }

        OrderPay pay = new OrderPay();
        pay.setOrderNo(orderNo);
        pay.setPayNo(1);   // 관리자 조회 쿼리가 OPay.PayNo = 1 을 대표 결제건으로 본다
        pay.setPayType(PAY_TYPE_EXCEL);
        pay.setPayDate(nowStr);
        pay.setPayAmt(sale);
        pay.setApprovDate(nowStr);
        pay.setReciptName(p.getOrderName());
        pay.setPgCode(PG_CODE_EXCEL);
        pay.setPgOrderNo(p.getChnlOrderNo());
        pay.setPgTradeNo(p.getChnlOrderNo());
        pay.setApprovNo(p.getChnlOrderNo());
        pay.setRegistId(registId);
        pay.setRegistName(registName);
        orderPayService.insert(pay);

        // 원본 보관 — 중복 판정의 기준. 같은 엑셀 주문번호가 이미 있으면 PK 위반(DuplicateKeyException)으로 이 행 전체가 롤백된다.
        mapper.insertExcelOrder(stagingRow(p, orderNo, registId, registName));
        return orderNo;
    }

    private static Map<String, Object> stagingRow(Parsed p, int orderNo, String registId, String registName) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderNo", p.getExcelOrderNo());
        m.put("chnlName", p.getChnlName());
        m.put("chnlOrderNo", p.getChnlOrderNo());
        m.put("orderName", p.getOrderName());
        m.put("orderPhoneNo", nz(p.getOrderPhoneNo()));
        m.put("orderMobileNo", nz(p.getOrderMobileNo()));
        m.put("recverName", p.getRecverName());
        m.put("recverPhoneNo", nz(p.getRecverPhoneNo()));
        m.put("recverMobileNo", nz(p.getRecverMobileNo()));
        m.put("recverZipcode", nz(p.getRecverZipcode()));
        m.put("recverAddr", p.getRecverAddr());
        m.put("deliMemo", nz(p.getDeliMemo()));
        m.put("shopProdCode", nz(p.getShopProdCode()));
        m.put("prodCode", nz(p.getProdCode()));
        m.put("prodName", p.getProdName());
        m.put("shopName", nz(p.getShopName()));
        m.put("supplyPrice", String.valueOf(p.getSupplyPrice()));
        m.put("salePrice", String.valueOf(p.getSalePrice()));
        m.put("buyPrice", String.valueOf(p.getBuyPrice()));
        m.put("prodQty", String.valueOf(p.getQty()));
        m.put("itemName", nz(p.getOptionName()));
        m.put("orderReciptDate", p.getOrderDate().toString());
        m.put("deliFeeType", nz(p.getDeliFeeTypeRaw()));
        m.put("deliFeeAmt", String.valueOf(p.getDeliFeeAmt()));
        m.put("promotFeeAmt", String.valueOf(p.getPromotFeeAmt()));
        m.put("shopionOrderNo", String.valueOf(orderNo));
        m.put("registId", registId);
        m.put("registName", registName);
        return m;
    }

    // ── 검사 ────────────────────────────────────────────────

    private OrderExcelResult check(InputStream in, HttpServletRequest request) {
        List<RawRow> raw;
        try {
            raw = parse(in);
        } catch (ExcelFileException e) {
            return OrderExcelResult.ofFileError(e.getMessage());
        }
        Ctx ctx = new Ctx(request);
        OrderExcelResult result = new OrderExcelResult();
        for (RawRow r : raw) result.getRows().add(validate(r, ctx));

        // 이미 처리된 엑셀 주문번호 — 한 번에 조회한다.
        Set<String> nos = new LinkedHashSet<>();
        for (OrderExcelRow row : result.getRows()) {
            if (row.getExcelOrderNo() != null && !row.getExcelOrderNo().isEmpty()) nos.add(row.getExcelOrderNo());
        }
        Set<String> existing = nos.isEmpty() ? Set.of() : new HashSet<>(mapper.selectExistingExcelOrderNos(nos));
        for (OrderExcelRow row : result.getRows()) {
            if (existing.contains(row.getExcelOrderNo())) row.setDuplicate(true);
        }
        result.summarize();
        return result;
    }

    private OrderExcelRow validate(RawRow raw, Ctx ctx) {
        OrderExcelRow row = new OrderExcelRow();
        row.setRowNo(raw.rowNo);
        Parsed p = new Parsed();

        String excelOrderNo = raw.get(H_ORDER_NO);
        row.setExcelOrderNo(excelOrderNo);
        if (excelOrderNo.isEmpty()) row.error(H_ORDER_NO + "를 입력하세요.");
        else if (excelOrderNo.length() > 20) row.error(H_ORDER_NO + "는 20자 이내여야 합니다: " + excelOrderNo);
        else if (!ctx.seenOrderNos.add(excelOrderNo)) row.error("파일 안에서 " + H_ORDER_NO + " " + excelOrderNo + "가 두 번 나옵니다. 앞의 행만 처리됩니다.");
        p.setExcelOrderNo(excelOrderNo);

        // 채널
        String chnlName = raw.get(H_CHNL_NAME);
        row.setChnlName(chnlName);
        p.setChnlName(chnlName);
        if (chnlName.isEmpty()) {
            row.error(H_CHNL_NAME + "을 입력하세요.");
        } else {
            List<String> codes = ctx.chnlCodes(chnlName);
            if (codes.isEmpty()) row.error("채널 '" + chnlName + "'을(를) 찾을 수 없습니다. 채널 관리의 채널 이름과 정확히 같아야 합니다.");
            else if (codes.size() > 1) row.error("채널 이름 '" + chnlName + "'이(가) 둘 이상입니다. 채널 이름을 구분할 수 있게 고쳐 주세요.");
            else {
                String code = codes.get(0);
                if (ctx.scope.chnlCode() != null && !ctx.scope.chnlCode().equals(code)) {
                    row.error("다른 채널('" + chnlName + "')의 주문은 등록할 수 없습니다.");
                } else {
                    p.setChnlCode(code);
                }
            }
        }

        String chnlOrderNo = raw.get(H_CHNL_ORDER_NO);
        if (chnlOrderNo.isEmpty() || chnlOrderNo.equals("0")) row.error(H_CHNL_ORDER_NO + "를 입력하세요.");
        else if (chnlOrderNo.length() > 50) row.error(H_CHNL_ORDER_NO + "는 50자 이내여야 합니다.");
        p.setChnlOrderNo(chnlOrderNo);

        // 주문자·수취인
        p.setOrderName(requiredText(row, raw, H_ORDER_NAME, 50));
        p.setOrderPhoneNo(optionalText(row, raw, H_ORDER_PHONE, 50));
        p.setOrderMobileNo(optionalText(row, raw, H_ORDER_MOBILE, 50));
        p.setRecverName(requiredText(row, raw, H_RECVER_NAME, 50));
        p.setRecverPhoneNo(optionalText(row, raw, H_RECVER_PHONE, 50));
        p.setRecverMobileNo(optionalText(row, raw, H_RECVER_MOBILE, 50));
        if (p.getRecverPhoneNo().isEmpty() && p.getRecverMobileNo().isEmpty()) {
            row.error("수취인 전화번호나 휴대번호 중 하나는 입력하세요.");
        }
        p.setRecverZipcode(optionalText(row, raw, H_RECVER_ZIP, 50));
        p.setRecverAddr(requiredText(row, raw, H_RECVER_ADDR, 255));
        p.setDeliMemo(optionalText(row, raw, H_DELI_MEMO, 255));

        // 상품
        p.setShopProdCode(optionalText(row, raw, H_SHOP_PROD_CODE, 255));
        p.setProdCode(optionalText(row, raw, H_PROD_CODE, 50));
        if (!p.getProdCode().isEmpty() && p.getProdCode().length() != 10) {
            row.error(H_PROD_CODE + "는 10자리여야 합니다: " + p.getProdCode());
        }
        p.setProdName(requiredText(row, raw, H_PROD_NAME, 255));
        row.setProdName(p.getProdName());
        p.setShopName(optionalText(row, raw, H_SHOP_NAME, 500));
        p.setOptionName(optionalText(row, raw, H_OPTION, 512));

        // 금액·수량
        Integer supply = intCell(row, raw, H_SUPPLY, true, 0, Integer.MAX_VALUE);
        Integer sale = intCell(row, raw, H_SALE, true, 0, Integer.MAX_VALUE);
        Integer buy = intCell(row, raw, H_BUY, false, 0, Integer.MAX_VALUE);
        Integer qty = intCell(row, raw, H_QTY, true, 1, MAX_QTY);
        Integer deliAmt = intCell(row, raw, H_DELI_AMT, false, 0, Integer.MAX_VALUE);
        Integer promot = intCell(row, raw, H_PROMOT, false, 0, Integer.MAX_VALUE);
        // 0원은 증정품 같은 경우라 막지 않지만, 입력 실수일 수 있으니 눈에 띄게 경고한다.
        if (sale != null && sale == 0) row.warn(H_SALE + "이 0원입니다. 증정품이 아니라면 값을 확인하세요.");
        p.setSupplyPrice(supply == null ? 0 : supply);
        p.setSalePrice(sale == null ? 0 : sale);
        p.setBuyPrice(buy == null ? 0 : buy);
        p.setQty(qty == null ? 0 : qty);
        p.setDeliFeeAmt(deliAmt == null ? 0 : deliAmt);
        p.setPromotFeeAmt(promot == null ? 0 : promot);
        if (qty != null) {   // 단가×수량이 int 범위를 넘으면 저장할 수 없다
            for (Object[] f : new Object[][]{{H_SUPPLY, supply}, {H_SALE, sale}, {H_BUY, buy}}) {
                if (f[1] != null && (long) (Integer) f[1] * qty > Integer.MAX_VALUE) row.error(f[0] + "×" + H_QTY + " 금액이 너무 큽니다.");
            }
        }

        // 배송비 타입
        String deliRaw = raw.get(H_DELI_TYPE);
        if (deliRaw.isEmpty()) row.error(H_DELI_TYPE + "을 입력하세요.");
        p.setDeliFeeTypeRaw(deliRaw);
        p.setDeliFeeType(deliFeeType(deliRaw));

        // 주문 일자
        LocalDate date = parseDate(raw.get(H_ORDER_DATE));
        if (raw.get(H_ORDER_DATE).isEmpty()) {
            row.error(H_ORDER_DATE + "를 입력하세요.");
        } else if (date == null) {
            row.error(H_ORDER_DATE + " 형식이 올바르지 않습니다(yyyy-MM-dd): " + raw.get(H_ORDER_DATE));
        } else if (date.isAfter(LocalDate.now())) {
            row.error(H_ORDER_DATE + "는 오늘 이후일 수 없습니다: " + date);
        } else {
            if (date.isBefore(LocalDate.now().minusDays(OLD_ORDER_DAYS))) {
                row.warn(H_ORDER_DATE + " " + date + "는 " + OLD_ORDER_DAYS + "일보다 오래됐습니다. 날짜가 맞는지 확인하세요.");
            }
            p.setOrderDate(date);
        }

        // 상품 매칭 — 오류가 없는 행만(범위 검사 포함)
        if (row.getErrors().isEmpty()) matchProduct(row, p, ctx);

        if (row.getErrors().isEmpty()) row.setParsed(p);
        return row;
    }

    /** 상점상품코드 → 상품코드 → 상품명+옵션 규칙 순으로 판매 상품을 찾는다. 못 찾으면 경고만 하고 미매칭으로 저장한다. */
    private void matchProduct(OrderExcelRow row, Parsed p, Ctx ctx) {
        Prod matched = ctx.findProd(p);
        if (matched != null) {
            try {
                scopeGuard.requirePrdProd(ctx.request, matched.getProdCode());
            } catch (ResponseStatusException e) {
                row.error("다른 업체의 상품(" + matched.getProdCode() + ")이라 등록할 수 없습니다.");
                return;
            }
            p.setMatched(matched);
            return;
        }
        if (ctx.scope.shopCode() != null) {
            row.error("상품이 연결되지 않아 등록할 수 없습니다. 상점 상품 코드나 상품 코드를 확인하세요.");
            return;
        }
        row.warn("상품이 연결되지 않았습니다. 저장은 되지만 재고는 차감되지 않으니 상품 매칭 화면에서 연결하세요.");
    }

    // ── 셀 해석 ─────────────────────────────────────────────

    private static String requiredText(OrderExcelRow row, RawRow raw, String header, int max) {
        String v = raw.get(header);
        if (v.isEmpty()) row.error(header + "을(를) 입력하세요.");
        else if (v.length() > max) row.error(header + "은(는) " + max + "자 이내여야 합니다.");
        return v;
    }

    private static String optionalText(OrderExcelRow row, RawRow raw, String header, int max) {
        String v = raw.get(header);
        if (v.length() > max) row.error(header + "은(는) " + max + "자 이내여야 합니다.");
        return v;
    }

    /** 0 이상 정수. 천 단위 쉼표·소수점 .0·끝의 '원'을 허용한다. 잘못된 값은 그 행의 오류로 모은다(null 반환). */
    private static Integer intCell(OrderExcelRow row, RawRow raw, String header, boolean required, int min, int max) {
        String s = raw.get(header);
        if (s.isEmpty()) {
            if (required) row.error(header + "을(를) 입력하세요.");
            return required ? null : 0;
        }
        String t = s.replace(",", "").replace(" ", "");
        if (t.endsWith("원")) t = t.substring(0, t.length() - 1);
        try {
            BigDecimal d = new BigDecimal(t);
            if (d.stripTrailingZeros().scale() > 0) throw new NumberFormatException();
            long v = d.longValueExact();
            if (v < min || v > max) {
                row.error(header + " 값이 범위를 벗어났습니다: " + s + (max == Integer.MAX_VALUE ? " (" + min + " 이상)" : " (" + min + "~" + max + ")"));
                return null;
            }
            return (int) v;
        } catch (NumberFormatException | ArithmeticException e) {
            row.error(header + "은(는) " + (min >= 1 ? "1 이상" : "0 이상") + " 정수여야 합니다: " + s);
            return null;
        }
    }

    /** yyyy-MM-dd(시각이 붙어도 됨), yyyy/MM/dd, yyyy.MM.dd, yyyyMMdd. 해석할 수 없으면 null. */
    static LocalDate parseDate(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 10) t = t.substring(0, 10);
        t = t.replace('/', '-').replace('.', '-');
        try {
            if (t.matches("\\d{8}")) return LocalDate.parse(t, DateTimeFormatter.BASIC_ISO_DATE);
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 레거시 규칙: 선불·선결제·'배송비:유료' 는 선불, 착불은 착불, 그 밖은 무료. */
    static String deliFeeType(String raw) {
        String s = raw == null ? "" : raw;
        if (s.contains("선불") || s.contains("선결제") || s.contains("배송비:유료")) return "선불";
        if (s.contains("착불")) return "착불";
        return "무료";
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static int parseInt(String s, int dflt) {
        try { return s == null ? dflt : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return dflt; }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

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
                    String text = cellText(row.getCell(e.getKey()));
                    if (!text.isEmpty()) any = true;
                    values.put(e.getValue(), text);
                }
                if (!any) continue;   // 완전히 빈 행은 건너뛴다
                if (rows.size() >= MAX_ROWS) throw new ExcelFileException("한 번에 " + MAX_ROWS + "행까지만 올릴 수 있습니다. 나누어 올려 주세요.");
                rows.add(new RawRow(r + 1, values));
            }
            if (rows.isEmpty()) throw new ExcelFileException("등록할 주문 행이 없습니다(2행부터 데이터를 입력하세요).");
            return rows;
        } catch (ExcelFileException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelFileException("엑셀 파일(.xls, .xlsx)을 읽을 수 없습니다.");
        }
    }

    /** 셀을 화면에 보이는 값이 아니라 저장된 값 기준 문자열로: 숫자는 1.0E9 도 44,800 도 아닌 평문 정수/소수. */
    static String cellText(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    // ── 요청 하나 동안 쓰는 컨텍스트 ───────────────────────────

    private final class Ctx {
        final HttpServletRequest request;
        final DataScope scope;
        final Set<String> seenOrderNos = new HashSet<>();
        private final Map<String, List<String>> chnlByName = new HashMap<>();
        private final Map<String, Optional<Prod>> prodCache = new HashMap<>();

        Ctx(HttpServletRequest request) {
            this.request = request;
            this.scope = scopeGuard.scope(request);
        }

        /** 같은 채널 이름은 한 번만 조회한다. 서비스 제한 계정은 자기 서비스 채널만 본다. */
        List<String> chnlCodes(String name) {
            return chnlByName.computeIfAbsent(name, n -> mapper.selectChnlCodesByName(n, scope.svcCode()));
        }

        Prod findProd(Parsed p) {
            Prod bySlug = null;
            if (!p.getShopProdCode().isEmpty()) {
                bySlug = cached("S:" + p.getShopProdCode(), () -> {
                    Map<String, Object> q = new HashMap<>();
                    q.put("pShopProdCode", p.getShopProdCode());
                    return prodService.selectByShopProdCode(q);
                });
            }
            if (bySlug != null) return bySlug;
            if (!p.getProdCode().isEmpty()) {
                Prod byCode = cached("P:" + p.getProdCode(), () -> exactProd(p.getProdCode()));
                if (byCode != null) return byCode;
            }
            return cached("N:" + p.getProdName() + "\u0000" + nz(p.getOptionName()), () -> {
                ProdCodeMatch rule = prodCodeMatchMapper.selectByNameOption(nz(p.getProdName()), nz(p.getOptionName()));
                if (rule == null || rule.getProdCode() == null || rule.getProdCode().isBlank()) return null;
                return exactProd(rule.getProdCode());
            });
        }

        private Prod cached(String key, java.util.function.Supplier<Prod> loader) {
            return prodCache.computeIfAbsent(key, k -> Optional.ofNullable(loader.get())).orElse(null);
        }

        private Prod exactProd(String prodCode) {
            Map<String, Object> q = new HashMap<>();
            q.put("pProdCode", prodCode);
            List<Prod> list = prodService.selectList(q);
            if (list == null) return null;
            for (Prod prod : list) if (prodCode.equals(prod.getProdCode())) return prod;
            return null;
        }
    }
}
