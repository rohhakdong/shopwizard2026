package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.ship.model.InvoiceExcelResult;
import com.shopwizard.ship.model.SlnkResendRequest;
import com.shopwizard.ship.service.InvoiceExcelService;
import com.shopwizard.ship.service.SlnkInvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * 송장입력 화면의 엑셀 일괄 입력과 샵링커 송장 재전송. 데이터 범위는 줄 단위로 건다 — 상점·채널·서비스 제한 계정은 자기 줄만 올리고 다시 보낼 수 있다
 * ({@link DataScopeGuard#requireLine}). 멀티파트라 등록자는 세션에서 직접 읽는다.
 */
@RestController
@RequestMapping("/ship/ship-direct")
@RequiredArgsConstructor
public class InvoiceExcelController {

    private final InvoiceExcelService excelService;
    private final SlnkInvoiceService slnkInvoice;
    private final DataScopeGuard scopeGuard;

    @GetMapping("/invoice-excel/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("invoiceExcel.xls", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(excelService.buildTemplate());
    }

    @PostMapping("/invoice-excel/preview")
    public InvoiceExcelResult preview(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return excelService.preview(file.getInputStream(), allowed(request));
    }

    @PostMapping("/invoice-excel/import")
    public InvoiceExcelResult importFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        requireExcel(file);
        return excelService.importFile(file.getInputStream(), allowed(request), AuthSession.actor(request));
    }

    /** 송장은 저장됐지만 샵링커로 나갔다는 성공 이력이 없는 줄(최근 14일) — 범위 조건은 DataScopeFilter 가 GET 파라미터로 고정한다. */
    @GetMapping("/invoice-slnk/pending")
    public Map<String, Object> pending(@RequestParam Map<String, String> p) {
        return slnkInvoice.pending(p.get("pShopCode"), p.get("pChnlCode"), p.get("pSvcCode"));
    }

    /** 선택한 줄의 송장을 샵링커로 다시 보낸다(최대 {@value SlnkInvoiceService#MAX_RESEND}줄). */
    @PostMapping("/invoice-slnk/resend")
    public Map<String, Object> resend(@RequestBody SlnkResendRequest req, HttpServletRequest request) {
        if (req.getLines() == null || req.getLines().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다시 보낼 줄을 선택하세요.");
        if (req.getLines().size() > SlnkInvoiceService.MAX_RESEND) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 " + SlnkInvoiceService.MAX_RESEND + "줄까지 다시 보낼 수 있습니다.");
        }
        scopeGuard.requireLines(request, req.getLines(), SlnkResendRequest.Line::getOrderNo, SlnkResendRequest.Line::getOrderProdNo);
        java.util.List<int[]> keys = new java.util.ArrayList<>();
        for (SlnkResendRequest.Line l : req.getLines()) {
            if (l.getOrderNo() == null || l.getOrderProdNo() == null || l.getOrderChangeNo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문번호·상품번호·변경번호가 필요합니다.");
            }
            keys.add(new int[]{l.getOrderNo(), l.getOrderProdNo(), l.getOrderChangeNo()});
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", slnkInvoice.mode());
        out.put("results", slnkInvoice.resend(keys, AuthSession.actor(request)));
        return out;
    }

    /** 줄이 내 데이터 범위인지(예외 대신 true/false) — 엑셀 검사가 행마다 묻는다. */
    private BiPredicate<Integer, Integer> allowed(HttpServletRequest request) {
        return (orderNo, orderProdNo) -> {
            try {
                scopeGuard.requireLine(request, orderNo, orderProdNo);
                return true;
            } catch (ResponseStatusException e) {
                return false;
            }
        };
    }

    private static void requireExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "엑셀 파일을 선택하세요.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "엑셀 파일(.xls, .xlsx)만 올릴 수 있습니다.");
    }
}
