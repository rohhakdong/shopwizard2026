package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.profile.service.CustInfoLogService;
import com.shopwizard.ship.model.GiftOptionUpdate;
import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.service.ShipGiftOptionRunService;
import com.shopwizard.ship.service.ShipPrintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 발주내역(ship-print-list)·발주내역2(ship-print-detail) 화면의 백엔드. 수령인·주소·전화번호가 나오므로
 * 상점 단위로 데이터 범위를 걸고({@link OrderMasterGuard#requireShop}), 목록·엑셀을 볼 때마다 개인정보 조회 기록을 남긴다
 * (레거시 {@code custInfoLogService.runInsertCustInfoLog} 와 같은 테이블 shopion.tPrfCustInfoLog).
 * 데이터를 읽은 요청만 기록하고, 기록이 실패해도 조회는 막지 않는다(오류 로그만).
 */
@Slf4j
@RestController
@RequestMapping("/ship/print")
@RequiredArgsConstructor
public class ShipPrintController {

    private final ShipPrintService service;
    private final ShipGiftOptionRunService runService;
    private final CustInfoLogService logService;
    private final OrderMasterGuard guard;

    /** 발주내역 목록 — {total, rows}. */
    @GetMapping("/list")
    public ShipPrintService.Page list(@RequestParam Map<String, String> p, HttpServletRequest request) {
        guard.requireShop(request, p.get("pShopCode"));
        ShipPrintService.Page page = service.list1(p.get("pShopCode"), p.get("pSvcCode"), p.get("pShipCheckStartDate"), p.get("pShipCheckEndDate"),
                intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
        logAccess(request, "shipPrintService.list1", p);
        return page;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam Map<String, String> p, HttpServletRequest request) {
        guard.requireShop(request, p.get("pShopCode"));
        ShipPrintService.Download file = service.excel1(p.get("pShopCode"), p.get("pSvcCode"), p.get("pShipCheckStartDate"), p.get("pShipCheckEndDate"), LocalDateTime.now());
        logAccess(request, "shipPrintService.excel1", p);
        return download(file);
    }

    /** 발주내역2 목록 — {total, rows}. */
    @GetMapping("/list2")
    public ShipPrintService.Page list2(@RequestParam Map<String, String> p, HttpServletRequest request) {
        guard.requireShop(request, p.get("pShopCode"));
        ShipPrintService.Page page = service.list2(p.get("pShopCode"), p.get("pSvcCode"), p.get("pDateType"), p.get("pStartDate"), p.get("pEndDate"),
                p.get("pChnlCode"), p.get("pShopMDCode"), p.get("pProdName"), intOrNull(p.get("pPageOffset")), intOrNull(p.get("pPageSize")));
        logAccess(request, "shipPrintService.list2", p);
        return page;
    }

    @GetMapping("/excel2")
    public ResponseEntity<byte[]> excel2(@RequestParam Map<String, String> p, HttpServletRequest request) {
        guard.requireShop(request, p.get("pShopCode"));
        ShipPrintService.Download file = service.excel2(p.get("pShopCode"), p.get("pSvcCode"), p.get("pDateType"), p.get("pStartDate"), p.get("pEndDate"),
                p.get("pChnlCode"), p.get("pShopMDCode"), p.get("pProdName"));
        logAccess(request, "shipPrintService.excel2", p);
        return download(file);
    }

    /** 발주내역2생성 — 상점·발주일 기간의 발주서 행에 채널상품번호·사은품 문자열·옵션금액을 다시 채운다. */
    @PostMapping("/gift-option-run")
    public ShipGiftOptionRunService.Result giftOptionRun(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String shopCode = body == null ? null : body.get("shopCode");
        guard.requireShop(request, shopCode);
        AuthSession.Actor actor = AuthSession.actor(request);
        return runService.run(shopCode, body.get("startDate"), body.get("endDate"),
                actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    /** 이 상점의 발주내역2생성 실행 이력(최신순). */
    @GetMapping("/gift-option-runs")
    public List<GiftOptionUpdate> giftOptionRuns(@RequestParam("pShopCode") String shopCode, HttpServletRequest request) {
        guard.requireShop(request, shopCode);
        return runService.history(shopCode, 30);
    }

    /** 한 실행의 주문별 처리 결과. */
    @GetMapping("/gift-option-run-details")
    public List<GiftOptionUpdateDetail> giftOptionRunDetails(@RequestParam("pShopCode") String shopCode,
                                                             @RequestParam("pUpdateId") Integer updateId, HttpServletRequest request) {
        guard.requireShop(request, shopCode);
        return runService.details(shopCode, updateId, 500);
    }

    private static ResponseEntity<byte[]> download(ShipPrintService.Download file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file.bytes());
    }

    private static final List<String> LOGGED = List.of("pShopCode", "pShipCheckStartDate", "pShipCheckEndDate", "pDateType", "pStartDate", "pEndDate",
            "pChnlCode", "pShopMDCode", "pProdName", "pPageOffset", "pPageSize");

    /** 개인정보 조회 기록 — 데이터를 실제로 읽은 뒤(검증 실패·거부된 요청은 제외)에 남기고, 기록이 실패해도 조회를 막지 않는다. */
    private void logAccess(HttpServletRequest request, String svcName, Map<String, String> p) {
        try {
            StringJoiner sj = new StringJoiner(", ", "{", "}");
            for (String k : LOGGED) {
                String v = p.get(k);
                if (v != null && !v.isBlank()) sj.add(k + "=" + v);
            }
            AuthSession.Actor actor = AuthSession.actor(request);
            logService.record(request.getRequestURI(), sj.toString(), svcName + "(map)", "shipPrintMapper", "tShpShipDirectPrint",
                    actor == null ? null : actor.loginId(), actor == null ? null : actor.name(), request.getRemoteAddr());
        } catch (RuntimeException e) {
            log.error("개인정보 조회 기록 실패 uri={} svc={}", request.getRequestURI(), svcName, e);
        }
    }

    private static Integer intOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 값이 올바르지 않습니다.");
        }
    }
}
