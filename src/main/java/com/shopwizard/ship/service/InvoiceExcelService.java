package com.shopwizard.ship.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.ship.mapper.SlnkInvoiceMapper;
import com.shopwizard.ship.model.InvoiceExcelResult;
import com.shopwizard.ship.model.SlnkInvoiceLine;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * 송장 엑셀 일괄 입력(송장입력 화면의 [송장 엑셀 업로드]). 레거시 invoiceExcelUpload.jsp 와 같은 열(주문번호·주문상품번호·주문변경번호·택배회사명·송장번호)을
 * 읽되, 검사(preview, 저장 없음)와 등록(import)을 나눈다. 등록은 서버가 다시 검사해 <b>정상 행만</b> 저장하고 오류 행은 건너뛴다.
 *
 * <p>저장은 기존 {@link ShipDirectService#updateInvoice}(발주확인 가드·배송시작·행 잠금)를 줄마다 호출하므로 줄마다 독립 커밋이다.
 * 저장이 커밋된 뒤 {@link SlnkInvoiceService#pushLine} 으로 샵링커에 알린다(시험 모드면 실제로 보내지 않는다).
 * 레거시보다 엄격하게: 택배회사명이 택배회사 마스터에 없으면 오류(레거시는 택배사 코드가 NULL 로 조용히 저장됐다), 송장번호 형식을 검사한다.</p>
 */
@Service
public class InvoiceExcelService {

    public static final int MAX_ROWS = 300;
    private static final Pattern DIGITS = Pattern.compile("[0-9]{1,10}");
    private static final Pattern INV_NO = Pattern.compile("[0-9A-Za-z]{1,30}");
    private static final Object IMPORT_LOCK = new Object();

    /** 열 이름(공백 제거·소문자) 별칭 → 표준 키. */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("주문번호", "order"), Map.entry("주문상품번호", "prod"), Map.entry("주문순번", "prod"),
            Map.entry("주문변경번호", "change"), Map.entry("변경번호", "change"),
            Map.entry("택배회사명", "comp"), Map.entry("택배회사명칭", "comp"), Map.entry("택배회사", "comp"), Map.entry("택배사", "comp"), Map.entry("택배사명", "comp"),
            Map.entry("송장번호", "inv"));
    private static final List<String> REQUIRED = List.of("order", "prod", "comp", "inv");

    private final ShipDirectService shipDirectService;
    private final SlnkInvoiceService slnkInvoice;
    private final SlnkInvoiceMapper mapper;

    public InvoiceExcelService(ShipDirectService shipDirectService, SlnkInvoiceService slnkInvoice, SlnkInvoiceMapper mapper) {
        this.shipDirectService = shipDirectService;
        this.slnkInvoice = slnkInvoice;
        this.mapper = mapper;
    }

    /** 파일에서 읽은 한 행(열 표준 키 → 값). */
    record RawRow(int rowNo, Map<String, String> values) {
        String get(String key) { return values.getOrDefault(key, ""); }
    }

    static final class ExcelFileException extends RuntimeException {
        ExcelFileException(String message) { super(message); }
    }

    // ── 양식 ─────────────────────────────────────────────────

    public byte[] buildTemplate() {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            Sheet sheet = wb.createSheet("송장");
            String[] headers = {"주문번호", "주문상품번호", "주문변경번호", "택배회사명", "송장번호"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(head);
                sheet.setColumnWidth(i, 18 * 256);
            }
            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("2415368");
            ex.createCell(1).setCellValue("1");
            ex.createCell(2).setCellValue("1");
            ex.createCell(3).setCellValue("CJ-GLS택배");
            ex.createCell(4).setCellValue("123456789012");
            Sheet guide = wb.createSheet("작성 안내");
            String[] lines = {
                    "1. 첫 시트(송장)의 1행은 머리글입니다. 2행부터 한 줄에 한 건씩 입력하세요(최대 " + MAX_ROWS + "행). 예시 행은 지우고 쓰세요.",
                    "2. 주문번호·주문상품번호·주문변경번호는 송장입력 화면의 같은 이름 열 값입니다. 주문변경번호를 비우면 1 로 봅니다.",
                    "3. 택배회사명은 택배회사 관리에 등록된 이름과 똑같이 입력해야 합니다.",
                    "4. 송장번호는 영문·숫자 30자 이내(공백·하이픈은 자동으로 뺍니다). 긴 숫자는 셀 서식을 '텍스트'로 두면 안전합니다.",
                    "5. 발주확인 상태인 줄만 등록됩니다. 이미 송장이 등록된 줄·상태가 다른 줄은 오류로 건너뜁니다.",
                    "6. 샵링커 주문인 줄은 등록 뒤 송장이 샵링커로도 전달됩니다(시험 모드에서는 실제로 보내지 않고 이력만 남깁니다)."};
            for (int i = 0; i < lines.length; i++) guide.createRow(i).createCell(0).setCellValue(lines[i]);
            guide.setColumnWidth(0, 110 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("양식을 만들 수 없습니다.", e);
        }
    }

    // ── 검사·등록 ─────────────────────────────────────────────

    /** 검사만 한다(DB 를 바꾸지 않는다). allowed: (주문번호, 주문상품번호) 가 내 데이터 범위인가. */
    public InvoiceExcelResult preview(InputStream in, BiPredicate<Integer, Integer> allowed) {
        InvoiceExcelResult result = base();
        List<RawRow> raw;
        try {
            raw = parse(in);
        } catch (ExcelFileException e) {
            result.setFileError(e.getMessage());
            result.setMessage(e.getMessage());
            return result;
        }
        validate(raw, allowed, result);
        result.setMessage("검사 결과: 정상 " + result.getOkCount() + "건, 오류 " + result.getErrorCount() + "건"
                + (result.isImportable() ? " — [등록]을 누르면 정상 행만 저장합니다." : ""));
        return result;
    }

    /** 다시 검사한 뒤 정상 행만 저장하고, 저장이 끝난 줄마다 샵링커 전송을 시도한다. */
    public InvoiceExcelResult importFile(InputStream in, BiPredicate<Integer, Integer> allowed, AuthSession.Actor actor) {
        InvoiceExcelResult result = base();
        List<RawRow> raw;
        try {
            raw = parse(in);
        } catch (ExcelFileException e) {
            result.setFileError(e.getMessage());
            result.setMessage(e.getMessage());
            return result;
        }
        synchronized (IMPORT_LOCK) {
            validate(raw, allowed, result);
            String actorId = actor == null ? null : actor.loginId();
            String actorName = actor == null ? null : actor.name();
            int saved = 0;
            for (InvoiceExcelResult.Row r : result.getRows()) {
                if (!"OK".equals(r.getStatus())) continue;
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("orderNo", r.getOrderNo());
                    params.put("orderProdNo", r.getOrderProdNo());
                    params.put("orderChangeNo", r.getOrderChangeNo());
                    params.put("deliCompName", r.getDeliCompName());
                    params.put("invNo", r.getInvNo());
                    params.put("changeId", actorId);
                    params.put("changeName", actorName);
                    int updated = shipDirectService.updateInvoice(params);   // 줄마다 독립 커밋
                    if (updated <= 0) {
                        fail(r, "저장되지 않았습니다(그 사이 상태가 바뀌었습니다).");
                        continue;
                    }
                    saved++;
                    r.setResultMessage("저장 완료");
                    SlnkInvoiceService.Result s = slnkInvoice.pushLine(r.getOrderNo(), r.getOrderProdNo(), r.getOrderChangeNo(), actor);
                    r.setSlnkStatus(s.status());
                    r.setSlnkMessage(s.message());
                } catch (RuntimeException e) {
                    fail(r, "저장 오류: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            }
            result.setSavedCount(saved);
            result.setImported(true);
            result.setOkCount((int) result.getRows().stream().filter(x -> "OK".equals(x.getStatus())).count());
            result.setErrorCount(result.getRows().size() - result.getOkCount());
            result.setImportable(false);
            result.setMessage("등록 결과: 저장 " + saved + "건, 오류 " + result.getErrorCount() + "건");
        }
        return result;
    }

    private InvoiceExcelResult base() {
        InvoiceExcelResult r = new InvoiceExcelResult();
        r.setSlnkMode(slnkInvoice.mode());
        return r;
    }

    private static void fail(InvoiceExcelResult.Row r, String message) {
        r.setStatus("ERROR");
        r.getErrors().add(message);
        r.setResultMessage(null);
    }

    /** 행마다 형식·줄 존재·상태·택배회사·범위·파일 안 중복을 검사한다. */
    void validate(List<RawRow> raw, BiPredicate<Integer, Integer> allowed, InvoiceExcelResult result) {
        Set<String> seen = new HashSet<>();
        Map<String, Boolean> compCache = new HashMap<>();
        for (RawRow rr : raw) {
            InvoiceExcelResult.Row r = new InvoiceExcelResult.Row();
            r.setRowNo(rr.rowNo());
            r.setDeliCompName(rr.get("comp").trim());
            String inv = rr.get("inv").replaceAll("[\\s-]", "");
            r.setInvNo(inv);
            Integer order = number(rr.get("order"), "주문번호", r, true);
            Integer prod = number(rr.get("prod"), "주문상품번호", r, true);
            Integer change = rr.get("change").isBlank() ? Integer.valueOf(1) : number(rr.get("change"), "주문변경번호", r, false);
            r.setOrderNo(order);
            r.setOrderProdNo(prod);
            r.setOrderChangeNo(change);
            if (r.getDeliCompName().isEmpty()) r.getErrors().add("택배회사명을 입력하세요.");
            if (inv.isEmpty()) r.getErrors().add("송장번호를 입력하세요.");
            else if (!INV_NO.matcher(inv).matches()) r.getErrors().add("송장번호는 영문·숫자 30자 이내여야 합니다(공백·하이픈은 자동으로 뺍니다).");

            if (r.getErrors().isEmpty()) {
                String key = order + "-" + prod + "-" + change;
                if (!seen.add(key)) {
                    r.getErrors().add("파일에 같은 줄(주문번호·상품번호·변경번호)이 이미 있습니다.");
                } else if (!allowed.test(order, prod)) {
                    r.getErrors().add("처리할 수 없는 줄입니다(데이터 범위).");
                } else {
                    SlnkInvoiceLine line = mapper.selectLine(order, prod, change);
                    if (line == null) r.getErrors().add("발주서 줄을 찾을 수 없습니다.");
                    else if ("배송시작".equals(line.getOrderState())) r.getErrors().add("이미 송장이 등록된 줄입니다.");
                    else if (!"발주확인".equals(line.getOrderState())) r.getErrors().add("발주확인 상태가 아닙니다(현재: " + line.getOrderState() + ").");
                    if (!compCache.computeIfAbsent(r.getDeliCompName(), n -> mapper.countDeliComp(n) > 0)) {
                        r.getErrors().add("택배회사명 '" + r.getDeliCompName() + "' 이(가) 택배회사 관리에 없습니다.");
                    }
                }
            }
            r.setStatus(r.getErrors().isEmpty() ? "OK" : "ERROR");
            result.getRows().add(r);
        }
        result.setOkCount((int) result.getRows().stream().filter(x -> "OK".equals(x.getStatus())).count());
        result.setErrorCount(result.getRows().size() - result.getOkCount());
        result.setImportable(result.getOkCount() > 0);
    }

    private static Integer number(String text, String label, InvoiceExcelResult.Row r, boolean required) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            if (required) r.getErrors().add(label + "를 입력하세요.");
            return null;
        }
        if (!DIGITS.matcher(t).matches()) {
            r.getErrors().add(label + "는 숫자여야 합니다.");
            return null;
        }
        long v = Long.parseLong(t);
        if (v < 1 || v > Integer.MAX_VALUE) {
            r.getErrors().add(label + " 값이 올바르지 않습니다.");
            return null;
        }
        return (int) v;
    }

    // ── 엑셀 읽기 ─────────────────────────────────────────────

    List<RawRow> parse(InputStream in) {
        try (Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0) throw new ExcelFileException("시트가 없는 엑셀 파일입니다.");
            Sheet sheet = wb.getSheetAt(0);
            Row head = sheet.getRow(sheet.getFirstRowNum());
            if (head == null) throw new ExcelFileException("머리글 행이 없습니다. 양식을 다시 받아 작성해 주세요.");
            DataFormatter fmt = new DataFormatter();
            Map<Integer, String> colKey = new HashMap<>();
            for (Cell c : head) {
                String key = ALIASES.get(fmt.formatCellValue(c).replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
                if (key != null) colKey.putIfAbsent(c.getColumnIndex(), key);
            }
            List<String> missing = new ArrayList<>();
            for (String req : REQUIRED) if (!colKey.containsValue(req)) missing.add(switch (req) {
                case "order" -> "주문번호";
                case "prod" -> "주문상품번호";
                case "comp" -> "택배회사명";
                default -> "송장번호";
            });
            if (!missing.isEmpty()) {
                throw new ExcelFileException("필수 열을 찾을 수 없습니다: " + String.join(", ", missing) + ". 첫 시트의 1행이 양식의 머리글인지 확인해 주세요.");
            }
            List<RawRow> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                boolean any = false;
                for (Map.Entry<Integer, String> e : colKey.entrySet()) {
                    String text = cellText(row.getCell(e.getKey()));
                    if (!text.isEmpty()) any = true;
                    values.put(e.getValue(), text);
                }
                if (!any) continue;   // 완전히 빈 행은 건너뛴다
                if (rows.size() >= MAX_ROWS) throw new ExcelFileException("한 번에 " + MAX_ROWS + "행까지만 올릴 수 있습니다. 나누어 올려 주세요.");
                rows.add(new RawRow(r + 1, values));
            }
            if (rows.isEmpty()) throw new ExcelFileException("등록할 행이 없습니다(2행부터 데이터를 입력하세요).");
            return rows;
        } catch (ExcelFileException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelFileException("엑셀 파일(.xls, .xlsx)을 읽을 수 없습니다.");
        }
    }

    /** 저장된 값 기준 문자열 — 숫자 송장번호가 1.0E11 이 되지 않게 평문 정수로. */
    static String cellText(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
