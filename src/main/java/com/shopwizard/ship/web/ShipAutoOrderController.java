package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.service.OrderFeeApplyService;
import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.ship.service.ShipAutoOrderService;
import com.shopwizard.ship.service.ShipDataBuildService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 자동발주 작업 화면(ship-auto-order.js)의 백엔드.
 * 상점 단위 작업이라 {@link OrderMasterGuard#requireShop} 으로 데이터 범위를 건다 — 상점 제한 계정은 자기 상점만,
 * 서비스 제한 계정은 자기 서비스의 상점만, 채널 제한 계정은 이 기능을 쓰지 못한다.
 * 출고지시·발주확인은 기존 {@code /ship/ship-direct/issue-batch}·{@code check-batch} 를 그대로 쓴다.
 */
@RestController
@RequestMapping("/ship/auto-order")
@RequiredArgsConstructor
public class ShipAutoOrderController {

    private final ShipAutoOrderService service;
    private final ShipDataBuildService buildService;
    private final OrderFeeApplyService feeApplyService;
    private final OrderMasterGuard guard;

    /** 현황 카드: 지불완료·상품 미매칭·발주단가 미매칭·출고지시·발주서 대기. */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam("pShopCode") String shopCode, HttpServletRequest request) {
        guard.requireShop(request, shopCode);
        return service.summary(shopCode);
    }

    /** 수수료 규칙 적용 — 지불완료 라인의 배송비·판촉비·공급가를 배송비·판촉비 규칙대로 고친다(출고지시 전에 실행). */
    @PostMapping("/apply-fee")
    public OrderFeeApplyService.Result applyFee(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String shopCode = body == null ? null : body.get("shopCode");
        guard.requireShop(request, shopCode);
        AuthSession.Actor actor = AuthSession.actor(request);
        return feeApplyService.apply(shopCode, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    /** 배송 데이터 생성(출고지·사은품·묶음배송·출력순서). 몇 번을 다시 실행해도 같은 결과다. */
    @PostMapping("/build")
    public ShipDataBuildService.Result build(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String shopCode = body == null ? null : body.get("shopCode");
        guard.requireShop(request, shopCode);
        AuthSession.Actor actor = AuthSession.actor(request);
        return buildService.build(shopCode, actor == null ? null : actor.loginId(), actor == null ? null : actor.name());
    }

    /** 발주서 대기 행(쪽 나눔) — 발주확인 전, 원본 라인이 출고지시인 행. */
    @GetMapping("/sheet")
    public Map<String, Object> sheet(@RequestParam("pShopCode") String shopCode,
                                     @RequestParam(value = "pPageOffset", required = false) Integer offset,
                                     @RequestParam(value = "pPageSize", required = false) Integer limit,
                                     HttpServletRequest request) {
        guard.requireShop(request, shopCode);
        return service.sheet(shopCode, offset, limit);
    }

    /** 발주확인 대상 라인 — 화면이 이 목록으로 /ship/ship-direct/check-batch 를 호출한다. */
    @GetMapping("/pending-lines")
    public List<Map<String, Object>> pendingLines(@RequestParam("pShopCode") String shopCode, HttpServletRequest request) {
        guard.requireShop(request, shopCode);
        return service.pendingLines(shopCode);
    }

    /**
     * 발주서 엑셀 다운로드. 상태를 바꾸지 않는다(발주확인은 따로). pShipCheckNo 가 있으면 그 발주 목록 건을 다시 뽑는다.
     * 다시 뽑는 건의 상점은 발주 목록 헤더에서 읽으므로 pShopCode 없이도 호출되지만, 범위 검사는 그 헤더의 상점으로 한다.
     */
    @GetMapping("/sheet-excel")
    public ResponseEntity<byte[]> sheetExcel(@RequestParam(value = "pShopCode", required = false) String shopCode,
                                             @RequestParam(value = "pShipCheckNo", required = false) Integer shipCheckNo,
                                             HttpServletRequest request) {
        AuthSession.Actor actor = AuthSession.actor(request);
        String actorId = actor == null ? null : actor.loginId();
        String actorName = actor == null ? null : actor.name();
        ShipAutoOrderService.Download file;
        if (shipCheckNo == null) {
            guard.requireShop(request, shopCode);
            file = service.download(shopCode, null, actorId, actorName);
        } else {
            guard.requireShop(request, service.shopOfCheck(shipCheckNo));
            file = service.download(null, shipCheckNo, actorId, actorName);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file.bytes());
    }
}
