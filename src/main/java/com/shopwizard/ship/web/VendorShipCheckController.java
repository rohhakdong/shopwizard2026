package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.ship.service.ShipAutoOrderService;
import com.shopwizard.ship.service.VendorShipCheckService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 업체(상점·창고) 전용 발주 목록·발주서 다운로드. 관리자 API 와 분리된 {@code /vendor/**} 경로이고, 상점은 {@code /vendor/shop/**}(SHOP 세션),
 * 창고는 {@code /vendor/warehs/**}(WAREHS 세션) 인터셉터가 로그인을 확인한다. 허용 상점 집합은 항상 서버가 세션에서 계산한다
 * (상점 = 자기 상점, 창고 = 연결된 상점) — 요청의 상점 파라미터는 그 안에서만 좁히는 용도다.
 */
@RestController
@RequiredArgsConstructor
public class VendorShipCheckController {

    private final VendorShipCheckService service;

    // ── 상점 ────────────────────────────────────────────────────

    @GetMapping("/vendor/shop/ship-check/list")
    public Map<String, Object> shopList(@RequestParam(value = "pStartDate", required = false) String startDate,
                                        @RequestParam(value = "pEndDate", required = false) String endDate,
                                        @RequestParam(value = "pPageOffset", required = false) Integer offset,
                                        @RequestParam(value = "pPageSize", required = false) Integer limit,
                                        HttpServletRequest request) {
        return service.list(shopAllowed(request), null, startDate, endDate, offset, limit);
    }

    @GetMapping("/vendor/shop/ship-check/excel")
    public ResponseEntity<byte[]> shopExcel(@RequestParam(value = "pShipCheckNo", required = false) Integer shipCheckNo, HttpServletRequest request) {
        AuthSession.Actor actor = AuthSession.actor(request);
        return excel(service.download(shopAllowed(request), shipCheckNo, actor == null ? null : actor.loginId(), actor == null ? null : actor.name()));
    }

    // ── 창고 ────────────────────────────────────────────────────

    @GetMapping("/vendor/warehs/ship-check/shops")
    public List<Map<String, Object>> warehsShops(HttpServletRequest request) {
        return service.shopsOfWarehs(warehsCode(request));
    }

    @GetMapping("/vendor/warehs/ship-check/list")
    public Map<String, Object> warehsList(@RequestParam(value = "pShopCode", required = false) String shopCode,
                                          @RequestParam(value = "pStartDate", required = false) String startDate,
                                          @RequestParam(value = "pEndDate", required = false) String endDate,
                                          @RequestParam(value = "pPageOffset", required = false) Integer offset,
                                          @RequestParam(value = "pPageSize", required = false) Integer limit,
                                          HttpServletRequest request) {
        return service.list(service.allowedShopsOfWarehs(warehsCode(request)), shopCode, startDate, endDate, offset, limit);
    }

    @GetMapping("/vendor/warehs/ship-check/excel")
    public ResponseEntity<byte[]> warehsExcel(@RequestParam(value = "pShipCheckNo", required = false) Integer shipCheckNo, HttpServletRequest request) {
        AuthSession.Actor actor = AuthSession.actor(request);
        return excel(service.download(service.allowedShopsOfWarehs(warehsCode(request)), shipCheckNo,
                actor == null ? null : actor.loginId(), actor == null ? null : actor.name()));
    }

    // ── 세션 → 허용 범위 ────────────────────────────────────────

    private static Set<String> shopAllowed(HttpServletRequest request) {
        Map<String, String> claims = AuthSession.get(request, AuthSession.Type.SHOP);
        String shop = claims == null ? null : claims.get("shopCode");
        if (shop == null || shop.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상점 정보가 없습니다.");
        return Set.of(shop.trim());
    }

    private static String warehsCode(HttpServletRequest request) {
        Map<String, String> claims = AuthSession.get(request, AuthSession.Type.WAREHS);
        String code = claims == null ? null : claims.get("warehsCode");
        if (code == null || code.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "창고 정보가 없습니다.");
        return code.trim();
    }

    private static ResponseEntity<byte[]> excel(ShipAutoOrderService.Download file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file.bytes());
    }
}
