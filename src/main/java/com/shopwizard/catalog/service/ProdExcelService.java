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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 상품 엑셀 일괄등록 — 예전 시스템에서 쓰던 35열 양식(prodExcel.xls)으로 상품을 한 번에 등록/수정한다.
 *
 * <ul>
 *   <li>상품코드가 비어 있으면 신규(코드는 서버가 채번), 값이 있으면 그 상품 수정(빈 칸은 기존 값 유지).</li>
 *   <li>열은 순서가 아니라 <b>헤더 이름</b>으로 찾는다. 먼저 {@link #preview}로 행별 검사를 보여주고,
 *       {@link #importAll}은 다시 검사해서 오류가 하나라도 있으면 아무것도 저장하지 않는다(전체 한 트랜잭션).</li>
 *   <li>실제 저장은 기존 {@link ProdService#insert}/{@link ProdService#update}를 그대로 쓴다 — 기본 옵션(20000) 생성,
 *       게시된 상품의 판매 상품 동기화가 단건 등록 화면과 똑같이 동작한다.</li>
 * </ul>
 */
@Service
public class ProdExcelService {

    public static final int MAX_ROWS = 1000;
    /** 한 번에 내려받을 수 있는 상품 수. */
    public static final int MAX_EXPORT = 5000;
    /** 엑셀 셀 하나에 넣을 수 있는 최대 글자 수. */
    static final int MAX_CELL_CHARS = 32_767;
    /** 내려받기에서 받는 화면 검색 조건 — 목록 조회와 같은 이름. 이 밖의 파라미터는 무시한다. */
    private static final String[] EXPORT_FILTERS = {"pProdCode", "pProdName", "pShopCode", "pSvcCode", "pSaleYn", "pApprovDate"};

    /** 양식 열(순서 = 예전 양식 그대로). {열 이름, 신규 등록 시, 수정 시, 설명}. 두 번째 시트 "작성안내"에도 쓴다. */
    static final String[][] COLUMNS = {
        {"상품코드", "비움", "필수", "비우면 신규 등록(코드는 자동 부여), 값이 있으면 그 상품을 수정합니다. 10자리 숫자."},
        {"상점상품코드", "선택", "선택", "상점 내부 상품코드. 다른 상품과 중복될 수 없습니다. 50자 이내."},
        {"상품명칭", "필수", "선택", "255자 이내."},
        {"상품전시명칭", "선택", "선택", "비우면 상품명칭을 사용합니다. 255자 이내."},
        {"분류코드", "필수", "선택", "13자리 최종 분류 코드(분류 관리 화면에서 확인). 상점의 서비스에 있는 분류여야 합니다."},
        {"분류명칭", "무시", "무시", "참고용입니다. 읽지 않습니다."},
        {"브랜드", "선택", "선택", "50자 이내. 브랜드 마스터에 없으면 경고만 표시하고 그대로 저장합니다."},
        {"모델", "선택", "선택", "50자 이내."},
        {"단위", "선택", "선택", "50자 이내."},
        {"상점코드", "필수", "선택", "12자리 상점코드."},
        {"상점명칭", "무시", "무시", "참고용입니다. 읽지 않습니다."},
        {"기존가격", "선택", "선택", "0 이상 정수. 비우면 0."},
        {"판매가격", "선택", "선택", "0 이상 정수. 비우면 0."},
        {"공급가격", "선택", "선택", "0 이상 정수. 비우면 0."},
        {"매입가격", "선택", "선택", "0 이상 정수. 비우면 0."},
        {"부가세율", "선택", "선택", "과세 / 면세 (숫자 10, 0도 가능). 비우면 과세."},
        {"제조사", "선택", "선택", "50자 이내. 제조사 마스터에 없으면 경고만 표시합니다."},
        {"원산지", "선택", "선택", "50자 이내. 원산지 마스터에 없으면 경고만 표시합니다."},
        {"속성명칭1", "선택", "선택", "옵션 이름(예: 사이즈). 50자 이내."},
        {"속성값1", "선택", "선택", "500자 이내."},
        {"속성명칭2", "선택", "선택", "50자 이내."},
        {"속성값2", "선택", "선택", "500자 이내."},
        {"속성명칭3", "선택", "선택", "50자 이내."},
        {"속성값3", "선택", "선택", "500자 이내."},
        {"속성명칭4", "선택", "선택", "50자 이내."},
        {"속성값4", "선택", "선택", "500자 이내."},
        {"이미지", "선택", "선택", "이미지 경로 또는 URL(/images/… 또는 http…). 썸네일은 만들지 않고 같은 경로를 모든 크기에 사용합니다."},
        {"배송비구분", "선택", "선택", "무료 / 선불 / 착불. 비우면 무료."},
        {"배송비", "선택", "선택", "0 이상 정수. 비우면 0."},
        {"상품설명", "선택", "선택", "HTML 사용 가능(약 21,000자 이내)."},
        {"판매상태", "선택", "선택", "승인대기 / 일시중지 / 정상판매 / 완전품절. 비우면 승인대기. 쇼핑몰 노출은 [승인] 뒤에 시작됩니다."},
        {"재고수량", "선택", "선택", "0 이상 정수. 비우면 0. 이미 게시된 상품이면 판매 상품의 재고에는 반영되지 않습니다."},
        {"메모01", "선택", "선택", "500자 이내."},
        {"수정", "무시", "무시", "예전 양식의 열입니다. 읽지 않습니다."},
        {"변경 일자", "무시", "무시", "예전 양식의 열입니다. 읽지 않습니다."},
    };

    private static final String H_PROD_CODE = "상품코드", H_SHOP_PROD_CODE = "상점상품코드", H_PROD_NAME = "상품명칭",
            H_DSPL_NAME = "상품전시명칭", H_CATE_CODE = "분류코드", H_BRAND = "브랜드", H_MODEL = "모델", H_UNIT = "단위",
            H_SHOP_CODE = "상점코드", H_LIST_PRICE = "기존가격", H_SALE_PRICE = "판매가격", H_SUPPLY_PRICE = "공급가격",
            H_BUY_PRICE = "매입가격", H_VAT = "부가세율", H_MAKER = "제조사", H_ORIGIN = "원산지", H_IMG = "이미지",
            H_DELI_TYPE = "배송비구분", H_DELI_AMT = "배송비", H_DESC = "상품설명", H_SALE_YN = "판매상태",
            H_QTY = "재고수량", H_MEMO = "메모01";
    private static final String[] H_ATTR_NAME = {"속성명칭1", "속성명칭2", "속성명칭3", "속성명칭4"};
    private static final String[] H_ATTR_VAL = {"속성값1", "속성값2", "속성값3", "속성값4"};

    /** 상품설명 컬럼은 TEXT(65,535바이트) — 넘으면 저장에서 실패하므로 미리 막는다. */
    private static final int MAX_DESC_BYTES = 65_535;
    private static final long FIRST_PROD_CODE = 1_000_000_000L;
    private static final Object IMPORT_LOCK = new Object();

    private final ProdExcelMapper mapper;
    private final ProdService prodService;
    private final ConstrValService constrValService;
    private final DataScopeGuard scopeGuard;
    private final TransactionTemplate tx;

    public ProdExcelService(ProdExcelMapper mapper, ProdService prodService, ConstrValService constrValService,
                            DataScopeGuard scopeGuard, PlatformTransactionManager txManager) {
        this.mapper = mapper;
        this.prodService = prodService;
        this.constrValService = constrValService;
        this.scopeGuard = scopeGuard;
        this.tx = new TransactionTemplate(txManager);
    }

    // ── 공개 API ────────────────────────────────────────────

    /** 검사만 한다. DB 는 바꾸지 않는다. */
    public ProdExcelResult preview(InputStream in, HttpServletRequest request) {
        return check(in, request);
    }

    /**
     * 다시 검사한 뒤 오류가 하나도 없을 때만 전부 저장한다(한 트랜잭션). 신규 상품코드는 최댓값+1 로 채번하므로
     * 동시에 두 번 돌면 같은 코드를 받지 않게 락을 커밋이 끝날 때까지 잡는다.
     */
    public ProdExcelResult importAll(InputStream in, HttpServletRequest request) {
        synchronized (IMPORT_LOCK) {
            return tx.execute(status -> doImport(in, request));
        }
    }

    /** 빈 양식(.xls) — 첫 시트는 35열 헤더, 두 번째 시트는 작성 안내. */
    public byte[] buildTemplate() {
        return buildWorkbook(List.of(), List.of());
    }

    /** 현재 검색 조건의 상품 목록을 같은 양식으로 내려받는다 — 고쳐서 다시 올리면 수정 행으로 처리된다. */
    public byte[] export(Map<String, String> filters) {
        Map<String, Object> q = new HashMap<>();
        // 화면 검색 조건만 받는다 — 목록 쿼리의 정렬(sidx/sord)처럼 SQL 에 그대로 들어가는 파라미터는 절대 통과시키지 않는다.
        for (String key : EXPORT_FILTERS) {
            String v = filters.get(key);
            if (v != null && !v.isBlank()) q.put(key, v.trim());
        }
        q.put("pPageOffset", 0);
        q.put("pPageSize", MAX_EXPORT + 1);
        List<Prod> list = prodService.selectList(q);
        if (list.size() > MAX_EXPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "검색 결과가 " + MAX_EXPORT + "건을 넘습니다. 검색 조건을 좁혀 다시 시도해 주세요.");
        }

        List<ConstrVal> tax = loadCodes("cTaxType");
        List<ConstrVal> saleYn = loadCodes("cSaleYnType");
        List<Object[]> rows = new ArrayList<>();
        List<String> longDesc = new ArrayList<>();
        for (Prod p : list) {
            String desc = p.getProdDesc() == null ? "" : p.getProdDesc();
            if (desc.length() > MAX_CELL_CHARS) {   // 엑셀 셀 한도 — 자르면 다시 올릴 때 설명이 망가지므로 비워 둔다(빈 칸은 기존 값 유지)
                longDesc.add(p.getProdCode());
                desc = "";
            }
            rows.add(exportRow(p, desc, tax, saleYn));
        }
        List<String> notes = new ArrayList<>();
        notes.add("이 파일은 목록에서 내려받은 것입니다. 고쳐서 다시 올리면 상품코드가 있는 행은 수정으로 처리됩니다.");
        if (!longDesc.isEmpty()) {
            notes.add("상품설명이 엑셀 셀 한도(" + MAX_CELL_CHARS + "자)를 넘는 " + longDesc.size() + "건은 설명 칸을 비워 두었습니다. "
                    + "그 칸을 비운 채 올리면 기존 설명이 유지됩니다. 상품코드: "
                    + String.join(", ", longDesc.subList(0, Math.min(longDesc.size(), 20))) + (longDesc.size() > 20 ? " 외" : ""));
        }
        return buildWorkbook(rows, notes);
    }

    /** 양식 35열 순서(COLUMNS)대로 한 상품을 한 행으로. 코드 값은 다시 올릴 때 그대로 읽히도록 한글 이름으로 쓴다. */
    private Object[] exportRow(Prod p, String desc, List<ConstrVal> tax, List<ConstrVal> saleYn) {
        return new Object[]{
            p.getProdCode(), p.getShopProdCode(), p.getProdName(), p.getProdDsplName(), p.getCateCode(), p.getCateName(),
            p.getBrandName(), p.getModelName(), p.getProdUnit(), p.getShopCode(), p.getShopName(),
            p.getListPrice(), p.getSalePrice(), p.getSupplyPrice(), p.getBuyPrice(),
            codeLabel(p.getVatRate(), tax), p.getMakerName(), p.getOriginName(),
            p.getAttrName1(), p.getAttrValStr1(), p.getAttrName2(), p.getAttrValStr2(),
            p.getAttrName3(), p.getAttrValStr3(), p.getAttrName4(), p.getAttrValStr4(),
            p.getImgUrl(), p.getDeliFeeType(), p.getDeliFeeAmt(), desc, codeLabel(p.getSaleYn(), saleYn),
            p.getSupplyQty(), p.getMemo01(), "수정", p.getChangeDate(),
        };
    }

    /** 코드 값 → 한글 이름(코드 테이블에 없으면 값 그대로). 올릴 때 이 이름을 다시 코드 값으로 바꾼다. */
    private static String codeLabel(Object value, List<ConstrVal> codes) {
        if (value == null) return "";
        String s = String.valueOf(value);
        for (ConstrVal cv : codes) {
            if (s.equals(cv.getConstrVal())) return cv.getConstrValDesc() == null ? s : cv.getConstrValDesc();
        }
        return s;
    }

    private byte[] buildWorkbook(List<Object[]> dataRows, List<String> notes) {
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
                sheet.setColumnWidth(i, (COLUMNS[i][0].length() < 6 ? 14 : 18) * 256);
            }
            sheet.createFreezePane(0, 1);
            int r = 1;
            for (Object[] values : dataRows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < values.length; i++) {
                    Object v = values[i];
                    if (v == null) continue;
                    if (v instanceof Number n) row.createCell(i).setCellValue(n.doubleValue());
                    else if (!String.valueOf(v).isEmpty()) row.createCell(i).setCellValue(String.valueOf(v));
                }
            }

            Sheet guide = wb.createSheet("작성안내");
            Row gh = guide.createRow(0);
            String[] titles = {"열 이름", "신규 등록", "수정", "설명"};
            for (int i = 0; i < titles.length; i++) {
                Cell c = gh.createCell(i);
                c.setCellValue(titles[i]);
                c.setCellStyle(head);
            }
            for (int c = 0; c < COLUMNS.length; c++) {
                Row row = guide.createRow(c + 1);
                for (int i = 0; i < 4; i++) row.createCell(i).setCellValue(COLUMNS[c][i]);
            }
            int n = COLUMNS.length + 2;
            List<String> lines = new ArrayList<>(notes);
            lines.add("※ 첫 시트(Sheet1)의 1행은 헤더, 2행부터 상품 데이터입니다. 열 순서는 바뀌어도 되고 헤더 이름으로 찾습니다.");
            lines.add("※ 수정할 때 빈 칸은 기존 값을 유지합니다(엑셀로 값을 비워 지우는 것은 지원하지 않습니다).");
            lines.add("※ 한 번에 최대 " + MAX_ROWS + "행까지 올릴 수 있고, 오류가 한 행이라도 있으면 아무것도 등록되지 않습니다.");
            lines.add("※ 새로 등록한 상품은 승인 전 상태로 들어가며, [승인] 후에 쇼핑몰에 노출됩니다.");
            for (String line : lines) guide.createRow(n++).createCell(0).setCellValue(line);
            guide.setColumnWidth(0, 16 * 256);
            guide.setColumnWidth(1, 12 * 256);
            guide.setColumnWidth(2, 10 * 256);
            guide.setColumnWidth(3, 90 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 양식을 만들 수 없습니다.", e);
        }
    }

    // ── 등록 ────────────────────────────────────────────────

    private ProdExcelResult doImport(InputStream in, HttpServletRequest request) {
        ProdExcelResult result = check(in, request);
        if (!result.isImportable()) return result;

        AuthSession.Actor actor = AuthSession.actor(request);
        long last = lastProdCode();
        for (ProdExcelRow row : result.getRows()) {
            Prod p = row.getProd();
            if (ProdExcelRow.NEW.equals(row.getAction())) {
                String code = String.valueOf(++last);
                if (code.length() > 10) throw new IllegalStateException("상품코드를 더 이상 채번할 수 없습니다.");
                p.setProdCode(code);
                row.setProdCode(code);
                prodService.insert(p);
            } else {
                prodService.update(p);
            }
        }
        result.setImported(true);
        return result;
    }

    private long lastProdCode() {
        String max = mapper.selectMaxProdCode();
        if (max != null && max.trim().matches("\\d{1,10}")) return Long.parseLong(max.trim());
        return FIRST_PROD_CODE;
    }

    // ── 검사 ────────────────────────────────────────────────

    private ProdExcelResult check(InputStream in, HttpServletRequest request) {
        List<RawRow> raws;
        try {
            raws = parse(in);
        } catch (ExcelFileException e) {
            return ProdExcelResult.ofFileError(e.getMessage());
        }
        ProdExcelResult result = new ProdExcelResult();
        Ctx ctx = new Ctx(request);
        for (RawRow raw : raws) result.getRows().add(validateRow(raw, ctx));
        result.summarize();
        return result;
    }

    private ProdExcelRow validateRow(RawRow raw, Ctx c) {
        ProdExcelRow row = new ProdExcelRow();
        row.setRowNo(raw.rowNo);
        String code = raw.get(H_PROD_CODE);
        boolean update = !code.isEmpty();
        row.setAction(update ? ProdExcelRow.UPDATE : ProdExcelRow.NEW);
        row.setProdName(raw.get(H_PROD_NAME));

        Prod p;
        if (update) {
            row.setProdCode(code);
            p = loadForUpdate(code, row, c);
            if (p == null) return row;
            if (row.getProdName().isEmpty()) row.setProdName(p.getProdName());
        } else {
            p = new Prod();
        }

        applyCells(raw, p, row, c, update, code);
        if (row.getErrors().isEmpty()) {
            if (!update) applyNewDefaults(p);
            AuthSession.Actor a = c.actor;
            String id = a == null || a.loginId() == null ? "" : a.loginId();
            String name = a == null || a.name() == null ? "" : a.name();
            if (!update) { p.setRegistId(id); p.setRegistName(name); }
            p.setChangeId(id);
            p.setChangeName(name);
            row.setProd(p);
        }
        return row;
    }

    /** 수정 대상 상품을 읽어 온다. 없거나 권한이 없으면 오류를 달고 null. */
    private Prod loadForUpdate(String code, ProdExcelRow row, Ctx c) {
        if (!code.matches("\\d{10}")) {
            row.error("상품코드는 10자리 숫자여야 합니다. 신규 등록이면 상품코드를 비워 주세요.");
            return null;
        }
        if (!c.seenProdCodes.add(code)) row.error("같은 상품코드가 파일 안에 두 번 이상 있습니다.");
        if (mapper.countProd(code) == 0) {
            row.error("존재하지 않는 상품코드입니다. 신규 등록이면 상품코드를 비워 주세요.");
            return null;
        }
        try {
            scopeGuard.requireCatProd(c.request, code);
        } catch (RuntimeException e) {
            row.error("권한이 없는 상품입니다.");
            return null;
        }
        Prod p = prodService.select(code);
        if (p == null) {
            row.error("상품 정보를 읽을 수 없습니다.");
            return null;
        }
        if (mapper.countPublished(code) > 0) {
            row.warn("쇼핑몰에 게시된 상품입니다. 수정하면 판매 상품에 바로 반영됩니다(재고수량은 반영되지 않습니다).");
        }
        return p;
    }

    /** 셀 값을 상품에 옮긴다. 수정이면 값이 있는 칸만 덮어쓰고, 신규면 값이 있는 칸만 채운 뒤 기본값을 따로 넣는다. */
    private void applyCells(RawRow raw, Prod p, ProdExcelRow row, Ctx c, boolean update, String code) {
        // 목록에서 내려받은 파일을 그대로 다시 올려도 안전해야 한다 — 값이 기존과 같은 칸은 검사도 덮어쓰기도 하지 않는다
        // (예: 이미지를 같은 경로로 덮으면 6개 썸네일 경로가 사라지고, 최종 분류가 아닌 기존 분류·코드표에 없는 기존 값은 오류가 된다).
        String exCate = nz(p.getCateCode()), exImg = nz(p.getImgUrl());
        String exBrand = nz(p.getBrandName()), exMaker = nz(p.getMakerName()), exOrigin = nz(p.getOriginName());
        String exVat = codeLabel(p.getVatRate(), c.tax(constrValService));
        String exSale = codeLabel(p.getSaleYn(), c.saleYn(constrValService));
        String exDeli = nz(p.getDeliFeeType());

        // 상품명
        String name = raw.get(H_PROD_NAME);
        if (!name.isEmpty()) {
            if (tooLong(name, 255, "상품명칭", row)) { /* 오류 기록됨 */ } else p.setProdName(name);
        } else if (!update) {
            row.error("상품명칭은 필수입니다.");
        }
        String dspl = raw.get(H_DSPL_NAME);
        if (!dspl.isEmpty() && !tooLong(dspl, 255, "상품전시명칭", row)) p.setProdDsplName(dspl);

        // 상점상품코드 — 유니크
        String sku = raw.get(H_SHOP_PROD_CODE);
        if (!sku.isEmpty() && !tooLong(sku, 50, "상점상품코드", row)) {
            if (!c.seenShopProdCodes.add(sku)) {
                row.error("상점상품코드 '" + sku + "'가 파일 안에서 중복됩니다.");
            } else if (mapper.countShopProdCode(sku, update ? code : null) > 0) {
                row.error("상점상품코드 '" + sku + "'는 이미 다른 상품이 사용 중입니다.");
            } else {
                p.setShopProdCode(sku);
            }
        }

        // 상점
        String shopCode = raw.get(H_SHOP_CODE);
        boolean shopChanged = false;
        if (!shopCode.isEmpty()) {
            if (!update || !shopCode.equals(p.getShopCode())) {
                String svc = c.svcOf(shopCode, mapper);
                if (svc == null) {
                    row.error("존재하지 않는 상점코드입니다: " + shopCode);
                } else if (c.scope.shopCode() != null && !c.scope.shopCode().equals(shopCode)) {
                    row.error("권한이 없는 상점입니다: " + shopCode);
                } else if (c.scope.svcCode() != null && !c.scope.svcCode().equals(svc)) {
                    row.error("다른 서비스의 상점입니다: " + shopCode);
                } else {
                    p.setShopCode(shopCode);
                    shopChanged = update;
                }
            }
        } else if (!update) {
            row.error("상점코드는 필수입니다.");
        }

        // 분류 — 그 상점의 서비스에 있는 최종 분류여야 한다
        String cate = raw.get(H_CATE_CODE);
        if (cate.isEmpty() && !update) row.error("분류코드는 필수입니다.");
        boolean cateUnchanged = update && cate.equals(exCate);
        if ((!cate.isEmpty() && !cateUnchanged) || shopChanged) {
            String effectiveCate = cate.isEmpty() ? p.getCateCode() : cate;
            String svc = p.getShopCode() == null ? null : c.svcOf(p.getShopCode(), mapper);
            if (!cate.isEmpty() && !cate.matches("[A-Za-z0-9]{13}")) {
                row.error("분류코드는 13자리여야 합니다: " + cate);
            } else if (svc != null && effectiveCate != null && mapper.countLeafCate(svc, effectiveCate) == 0) {
                row.error("분류코드가 없거나 최종 분류가 아닙니다: " + effectiveCate);
            } else if (!cate.isEmpty() && svc != null) {
                p.setCateCode(cate);
            }
        }

        // 텍스트 열
        String brand = textCell(raw, H_BRAND, 50, row);
        if (brand != null) {
            p.setBrandName(brand);
            if (!brand.equals(exBrand)) c.warnMaster(row, "브랜드", brand, () -> mapper.countBrand(c.svcOfShop(p, mapper), brand));
        }
        String model = textCell(raw, H_MODEL, 50, row);
        if (model != null) p.setModelName(model);
        String unit = textCell(raw, H_UNIT, 50, row);
        if (unit != null) p.setProdUnit(unit);
        String maker = textCell(raw, H_MAKER, 50, row);
        if (maker != null) {
            p.setMakerName(maker);
            if (!maker.equals(exMaker)) c.warnMaster(row, "제조사", maker, () -> mapper.countMaker(maker));
        }
        String origin = textCell(raw, H_ORIGIN, 50, row);
        if (origin != null) {
            p.setOriginName(origin);
            if (!origin.equals(exOrigin)) c.warnMaster(row, "원산지", origin, () -> mapper.countOrigin(origin));
        }
        String memo = textCell(raw, H_MEMO, 500, row);
        if (memo != null) p.setMemo01(memo);
        for (int i = 0; i < 4; i++) {
            String attrName = textCell(raw, H_ATTR_NAME[i], 50, row);
            String attrVal = textCell(raw, H_ATTR_VAL[i], 500, row);
            setAttr(p, i, attrName, attrVal);
        }

        // 이미지 — 경로만 받는다(썸네일을 만들지 않으므로 모든 크기에 같은 경로)
        String img = raw.get(H_IMG);
        if (!img.isEmpty() && !img.equals(exImg) && !tooLong(img, 255, "이미지", row)) {
            if (!(img.startsWith("/images/") || img.startsWith("http://") || img.startsWith("https://"))) {
                row.warn("이미지 경로가 /images/ 또는 http(s):// 로 시작하지 않습니다: " + img);
            }
            p.setImgUrl(img);
            p.setImgUrl50(img); p.setImgUrl80(img); p.setImgUrl100(img);
            p.setImgUrl160(img); p.setImgUrl220(img); p.setImgUrl280(img);
        }

        // 상품설명(HTML) — 원문 그대로 저장, 화면 쪽이 정화해서 보여준다
        String desc = raw.get(H_DESC);
        if (!desc.isEmpty()) {
            if (desc.getBytes(StandardCharsets.UTF_8).length > MAX_DESC_BYTES) row.error("상품설명이 너무 깁니다(65,535바이트 초과).");
            else p.setProdDesc(desc);
        }

        // 숫자
        Integer v;
        if ((v = intCell(raw, H_LIST_PRICE, row)) != null) p.setListPrice(v);
        if ((v = intCell(raw, H_SALE_PRICE, row)) != null) p.setSalePrice(v);
        if ((v = intCell(raw, H_SUPPLY_PRICE, row)) != null) p.setSupplyPrice(v);
        if ((v = intCell(raw, H_BUY_PRICE, row)) != null) p.setBuyPrice(v);
        if ((v = intCell(raw, H_DELI_AMT, row)) != null) p.setDeliFeeAmt(v);
        if ((v = intCell(raw, H_QTY, row)) != null) p.setSupplyQty(v);

        // 코드 값 (한글 이름 또는 코드 값)
        String vat = codeCell(raw, H_VAT, c.tax(constrValService), row, exVat);
        if (vat != null) p.setVatRate(Integer.valueOf(vat));
        String deli = codeCell(raw, H_DELI_TYPE, c.deliFee(constrValService), row, exDeli);
        if (deli != null) p.setDeliFeeType(deli);
        String sale = codeCell(raw, H_SALE_YN, c.saleYn(constrValService), row, exSale);
        if (sale != null) p.setSaleYn(Integer.valueOf(sale));
    }

    /** 속성 i(0~3)의 이름/값을 상품에 옮긴다. null 이면(빈 칸·오류) 건드리지 않는다. */
    private static void setAttr(Prod p, int i, String name, String val) {
        switch (i) {
            case 0 -> { if (name != null) p.setAttrName1(name); if (val != null) p.setAttrValStr1(val); }
            case 1 -> { if (name != null) p.setAttrName2(name); if (val != null) p.setAttrValStr2(val); }
            case 2 -> { if (name != null) p.setAttrName3(name); if (val != null) p.setAttrValStr3(val); }
            default -> { if (name != null) p.setAttrName4(name); if (val != null) p.setAttrValStr4(val); }
        }
    }

    /** 신규 상품에 비어 있는 값의 기본값 — 단건 등록 화면의 기본값과 맞춘다. */
    private void applyNewDefaults(Prod p) {
        if (p.getProdDsplName() == null || p.getProdDsplName().isEmpty()) p.setProdDsplName(p.getProdName());
        if (p.getListPrice() == null) p.setListPrice(0);
        if (p.getSalePrice() == null) p.setSalePrice(0);
        if (p.getSupplyPrice() == null) p.setSupplyPrice(0);
        if (p.getBuyPrice() == null) p.setBuyPrice(0);
        if (p.getVatRate() == null) p.setVatRate(10);
        if (p.getSaleYn() == null) p.setSaleYn(-1);
        if (p.getSupplyQty() == null) p.setSupplyQty(0);
        if (p.getDeliFeeType() == null) p.setDeliFeeType(DEFAULT_DELI_FEE_TYPE);
        if (p.getDeliFeeAmt() == null) p.setDeliFeeAmt(0);
        if (p.getAdultYn() == null) p.setAdultYn(0);
        if (p.getLeadTime() == null) p.setLeadTime(0);
        if (p.getState() == null) p.setState(1);
    }

    private static final String DEFAULT_DELI_FEE_TYPE = "무료";

    // ── 셀 해석 ─────────────────────────────────────────────

    /** 값이 있으면 길이를 검사해 돌려주고, 비었거나 너무 길면(오류 기록) null. */
    private static String textCell(RawRow raw, String header, int max, ProdExcelRow row) {
        String s = raw.get(header);
        if (s.isEmpty()) return null;
        return tooLong(s, max, header, row) ? null : s;
    }

    private static boolean tooLong(String s, int max, String label, ProdExcelRow row) {
        if (s.length() <= max) return false;
        row.error(label + "은(는) " + max + "자 이내여야 합니다(현재 " + s.length() + "자).");
        return true;
    }

    private static Integer intCell(RawRow raw, String header, ProdExcelRow row) {
        String s = raw.get(header).replace(",", "").replace(" ", "");
        if (s.isEmpty()) return null;
        BigDecimal bd;
        try {
            bd = new BigDecimal(s);
        } catch (NumberFormatException e) {
            row.error(header + "은(는) 숫자여야 합니다: " + raw.get(header));
            return null;
        }
        if (bd.signum() < 0) { row.error(header + "은(는) 0 이상이어야 합니다."); return null; }
        if (bd.stripTrailingZeros().scale() > 0) { row.error(header + "은(는) 정수여야 합니다: " + raw.get(header)); return null; }
        if (bd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) { row.error(header + " 값이 너무 큽니다."); return null; }
        return bd.intValueExact();
    }

    /** 코드 이름/값을 코드 값으로. 비었거나 기존 값과 같으면(올리기 전 그대로) null — 건드리지 않는다. */
    private static String codeCell(RawRow raw, String header, List<ConstrVal> codes, ProdExcelRow row, String existingText) {
        String s = raw.get(header);
        if (s.isEmpty() || s.equals(existingText)) return null;
        String key = squash(s);
        for (ConstrVal cv : codes) {
            if (key.equals(squash(cv.getConstrValDesc())) || key.equals(squash(cv.getConstrVal()))) return cv.getConstrVal();
        }
        StringBuilder allowed = new StringBuilder();
        for (ConstrVal cv : codes) {
            if (allowed.length() > 0) allowed.append(" / ");
            allowed.append(cv.getConstrValDesc());
        }
        row.error(header + " 값이 올바르지 않습니다: " + s + " (사용 가능: " + allowed + ")");
        return null;
    }

    private static String nz(String s) { return s == null ? "" : s; }

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
            if (!colName.containsValue(squash(H_PROD_NAME))) {
                throw new ExcelFileException("'" + H_PROD_NAME + "' 열을 찾을 수 없습니다. 첫 시트의 1행이 양식의 헤더인지 확인해 주세요.");
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
                if (rows.size() >= MAX_ROWS) throw new ExcelFileException("한 번에 " + MAX_ROWS + "행까지만 올릴 수 있습니다.");
                rows.add(new RawRow(r + 1, values));
            }
            if (rows.isEmpty()) throw new ExcelFileException("등록할 상품 행이 없습니다(2행부터 데이터를 입력하세요).");
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
        final AuthSession.Actor actor;
        final DataScope scope;
        final Set<String> seenProdCodes = new HashSet<>();
        final Set<String> seenShopProdCodes = new HashSet<>();
        private final Map<String, String> svcByShop = new HashMap<>();
        private final Map<String, Boolean> masterExists = new HashMap<>();
        private List<ConstrVal> tax, deliFee, saleYn;

        Ctx(HttpServletRequest request) {
            this.request = request;
            this.actor = AuthSession.actor(request);
            this.scope = scopeGuard.scope(request);
        }

        String svcOf(String shopCode, ProdExcelMapper m) {
            if (!svcByShop.containsKey(shopCode)) svcByShop.put(shopCode, m.selectShopSvc(shopCode));
            return svcByShop.get(shopCode);
        }

        String svcOfShop(Prod p, ProdExcelMapper m) {
            return p.getShopCode() == null ? null : svcOf(p.getShopCode(), m);
        }

        /** 마스터에 없는 이름이면 경고(저장은 막지 않는다). 같은 이름은 한 번만 조회한다. */
        void warnMaster(ProdExcelRow row, String label, String name, java.util.function.IntSupplier count) {
            String key = label + "\u0000" + name;
            Boolean exists = masterExists.get(key);
            if (exists == null) {
                exists = count.getAsInt() > 0;
                masterExists.put(key, exists);
            }
            if (!exists) row.warn(label + " '" + name + "'이(가) 마스터에 없습니다(그대로 저장됩니다).");
        }

        List<ConstrVal> tax(ConstrValService s) { return tax != null ? tax : (tax = load(s, "cTaxType")); }
        List<ConstrVal> deliFee(ConstrValService s) { return deliFee != null ? deliFee : (deliFee = load(s, "cDeliFeeType")); }
        List<ConstrVal> saleYn(ConstrValService s) { return saleYn != null ? saleYn : (saleYn = load(s, "cSaleYnType")); }

        private List<ConstrVal> load(ConstrValService s, String constrCode) {
            return loadCodes(constrCode);
        }
    }

    private List<ConstrVal> loadCodes(String constrCode) {
        Map<String, Object> q = new HashMap<>();
        q.put("pConstrCode", constrCode);
        q.put("sidx", "ConstrValSeq");
        q.put("sord", "ASC");
        List<ConstrVal> list = constrValService.findConstrValList(q);
        return list == null ? List.of() : list;
    }
}
