package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipPrintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 발주내역·발주내역2 조회와 엑셀(읽기 전용). 상점은 필수 — 레거시는 상점 없이 조회하면 조용히 빈 화면이었지만
 * 여기서는 400 으로 안내한다. 데이터 범위(상점·서비스 제한)는 컨트롤러가 검사하고, 서비스 제한은 svcCode 로 SQL 에도 건다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipPrintService {

    static final int DEFAULT_PAGE = 50;
    static final int MAX_PAGE = 200;
    /** 엑셀 한 번에 뽑는 행 상한 — 메모리·응답 시간 보호. 넘으면 기간을 좁히라고 안내한다. */
    static final int MAX_EXCEL_ROWS = 50_000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ShipPrintMapper mapper;

    /** 목록 응답: 전체 건수와 이 쪽의 행. */
    public record Page(int total, List<Map<String, Object>> rows) {}

    /** 엑셀 응답: 파일명과 내용. */
    public record Download(String fileName, byte[] bytes) {}

    // ── 발주내역 ──────────────────────────────────────────────

    public Page list1(String shopCode, String svcCode, String start, String end, Integer offset, Integer limit) {
        String shop = requireShop(shopCode);
        String[] range = range(start, end);
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit < 1 ? DEFAULT_PAGE : Math.min(limit, MAX_PAGE);
        int total = mapper.selectCount(shop, blank(svcCode), range[0], range[1]);
        List<Map<String, Object>> rows = total == 0 ? List.of()
                : mapper.selectList(shop, blank(svcCode), range[0], range[1], off, lim).stream().map(ShipPrintRows::print1).toList();
        return new Page(total, rows);
    }

    public Download excel1(String shopCode, String svcCode, String start, String end, LocalDateTime now) {
        String shop = requireShop(shopCode);
        String[] range = range(start, end);
        if (mapper.selectCount(shop, blank(svcCode), range[0], range[1]) > MAX_EXCEL_ROWS) throw tooMany();
        List<Map<String, Object>> rows = mapper.selectList(shop, blank(svcCode), range[0], range[1], null, null).stream()
                .map(ShipPrintRows::print1).toList();
        return new Download(sanitize("발주내역(" + shopLabel(shop) + ")" + now.format(STAMP)) + ".xls", ShipPrintExcel.buildPrint1(rows));
    }

    // ── 발주내역2 ─────────────────────────────────────────────

    public Page list2(String shopCode, String svcCode, String dateType, String start, String end,
                      String chnlCode, String mdCode, String prodName, Integer offset, Integer limit) {
        String shop = requireShop(shopCode);
        String[] range = range(start, end);
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit < 1 ? DEFAULT_PAGE : Math.min(limit, MAX_PAGE);
        String type = dateType(dateType);
        int total = mapper.selectCount2(shop, blank(svcCode), type, range[0], range[1], blank(chnlCode), blank(mdCode), blank(prodName));
        List<Map<String, Object>> rows = total == 0 ? List.of()
                : mapper.selectList2(shop, blank(svcCode), type, range[0], range[1], blank(chnlCode), blank(mdCode), blank(prodName), off, lim)
                        .stream().map(ShipPrintRows::print2).peek(r -> r.remove("recverAddr")).toList();   // 화면에는 주소를 싣지 않는다(엑셀 합배송 판정용)
        return new Page(total, rows);
    }

    public Download excel2(String shopCode, String svcCode, String dateType, String start, String end,
                           String chnlCode, String mdCode, String prodName) {
        String shop = requireShop(shopCode);
        String[] range = range(start, end);
        String type = dateType(dateType);
        if (mapper.selectCount2(shop, blank(svcCode), type, range[0], range[1], blank(chnlCode), blank(mdCode), blank(prodName)) > MAX_EXCEL_ROWS) {
            throw tooMany();
        }
        List<Map<String, Object>> rows = mapper.selectList2(shop, blank(svcCode), type, range[0], range[1], blank(chnlCode), blank(mdCode), blank(prodName), null, null)
                .stream().map(ShipPrintRows::print2).toList();
        String label = shopLabel(shop);
        String period = range[0] == null ? "전체" : range[0] + "_" + range[1];
        return new Download(sanitize("발주내역2(" + label + ")_" + period) + ".xls", ShipPrintExcel.buildPrint2(label, rows));
    }

    // ── 공통 ──────────────────────────────────────────────────

    private String shopLabel(String shop) {
        String name = mapper.selectShopName(shop);
        return name == null || name.isBlank() ? shop : name;
    }

    static String requireShop(String shopCode) {
        String shop = shopCode == null ? "" : shopCode.trim();
        if (shop.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "상점을 선택하세요.");
        return shop;
    }

    /** 기간: 둘 다 비면 기간 조건 없음, 하나만 있거나 형식·순서가 틀리면 400. 반환은 {시작, 종료}(yyyy-MM-dd) 또는 {null, null}. */
    static String[] range(String start, String end) {
        String s = blank(start);
        String e = blank(end);
        if (s == null && e == null) return new String[]{null, null};
        if (s == null || e == null) throw new ResponseStatusException(BAD_REQUEST, "시작일과 종료일을 함께 입력하세요.");
        try {
            LocalDate ds = LocalDate.parse(s);
            LocalDate de = LocalDate.parse(e);
            if (ds.isAfter(de)) throw new ResponseStatusException(BAD_REQUEST, "시작일이 종료일보다 늦습니다.");
            return new String[]{ds.toString(), de.toString()};
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "날짜는 yyyy-MM-dd 형식으로 입력하세요.");
        }
    }

    /** 날짜 기준: S=발주일(기본), O=주문일. */
    static String dateType(String raw) {
        String t = blank(raw);
        if (t == null || "S".equals(t)) return "S";
        if ("O".equals(t)) return "O";
        throw new ResponseStatusException(BAD_REQUEST, "날짜 기준은 발주일(S) 또는 주문일(O) 이어야 합니다.");
    }

    private static ResponseStatusException tooMany() {
        return new ResponseStatusException(BAD_REQUEST, "엑셀은 한 번에 " + String.format("%,d", MAX_EXCEL_ROWS) + "행까지 받을 수 있습니다. 기간을 좁혀 주세요.");
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String sanitize(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }
}
