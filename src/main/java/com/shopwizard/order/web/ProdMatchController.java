package com.shopwizard.order.web;

import com.shopwizard.order.model.ProdMatchRequest;
import com.shopwizard.order.model.ProdMatchResult;
import com.shopwizard.order.service.ProdMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 샵링커/엑셀 수집 주문의 상품 매칭 (order-prod-match.js).
 * 매칭 = 미매칭 주문라인에 실제 상품을 연결하고 공급가/원가/마진/세율을 상품 기준으로 채우는 것.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/match")
public class ProdMatchController {

    private final ProdMatchService prodMatchService;

    @GetMapping("/unmatched/list")
    public List<Map<String, Object>> unmatchedList(@RequestParam Map<String, Object> params) {
        return prodMatchService.selectUnmatched(params);
    }

    @GetMapping("/unmatched/list/count")
    public int unmatchedCount(@RequestParam Map<String, Object> params) {
        return prodMatchService.selectUnmatchedCount(params);
    }

    @PostMapping("/apply")
    public ProdMatchResult apply(@RequestBody ProdMatchRequest req) {
        return prodMatchService.apply(req);
    }

    /** 미매칭 목록을 CSV(UTF-8 BOM)로 — Excel 에서 상품코드 칸을 채워 다시 업로드. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Map<String, Object> params) {
        List<Map<String, Object>> rows = prodMatchService.selectUnmatched(params);
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("상품명,옵션,채널,건수,대표주문번호,상품코드(여기에입력),상점상품코드(대안)\r\n");
        for (Map<String, Object> r : rows) {
            sb.append(csv(str(r.get("prodName")))).append(',')
              .append(csv(str(r.get("prodOption")))).append(',')
              .append(csv(str(r.get("chnlNames")))).append(',')
              .append(csv(str(r.get("cnt")))).append(',')
              .append(csv(str(r.get("sampleOrderNo")))).append(',')
              .append(",\r\n");   // 상품코드/상점상품코드 빈칸
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        h.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("prod-match_" + java.time.LocalDate.now().toString().replace("-", "") + ".csv")
                .build());
        return ResponseEntity.ok().headers(h).body(body);
    }

    /**
     * CSV 업로드 — 각 행의 상품코드(또는 상점상품코드)로 (상품명,옵션) 묶음을 매칭한다.
     * 헤더: 상품명,옵션,채널,건수,대표주문번호,상품코드,상점상품코드
     */
    @PostMapping("/upload")
    public List<ProdMatchResult> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "svcCode", required = false) String svcCode,
                                        @RequestParam(value = "pStartDate", required = false) String pStartDate,
                                        @RequestParam(value = "pEndDate", required = false) String pEndDate,
                                        @RequestParam(value = "pChnlCode", required = false) String pChnlCode,
                                        @RequestParam(value = "registId", required = false) String registId,
                                        @RequestParam(value = "registName", required = false) String registName) throws Exception {
        List<ProdMatchResult> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; line = stripBom(line);
                    if (line.contains("상품명")) continue; }   // 헤더 스킵
                if (line.isBlank()) continue;
                List<String> cols = parseCsvLine(line);
                String prodName    = col(cols, 0);
                String prodOption  = col(cols, 1);
                String prodCode    = col(cols, 5);
                String shopProdCode = col(cols, 6);
                if (prodName.isBlank() || (prodCode.isBlank() && shopProdCode.isBlank())) {
                    results.add(ProdMatchResult.fail(prodName, prodOption, "상품코드 또는 상점상품코드가 비어 있어 건너뜀"));
                    continue;
                }
                ProdMatchRequest req = new ProdMatchRequest();
                req.setProdName(prodName);
                req.setProdOption(prodOption);
                req.setProdCode(prodCode);
                req.setShopProdCode(shopProdCode);
                req.setSaveRule(true);
                req.setSvcCode(svcCode);
                req.setPStartDate(pStartDate);
                req.setPEndDate(pEndDate);
                req.setPChnlCode(pChnlCode);
                req.setRegistId(registId);
                req.setRegistName(registName);
                try {
                    results.add(prodMatchService.apply(req));
                } catch (Exception e) {
                    results.add(ProdMatchResult.fail(prodName, prodOption, e.getMessage()));
                }
            }
        }
        return results;
    }

    // ── CSV 유틸 ─────────────────────────────────────────────────────

    private static String stripBom(String s) {
        return (s != null && s.startsWith("﻿")) ? s.substring(1) : s;
    }

    /** 따옴표 감싼 값·이스케이프("")·콤마 포함 값을 처리하는 최소 CSV 라인 파서. */
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String col(List<String> cols, int i) {
        return i < cols.size() ? cols.get(i).trim() : "";
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
