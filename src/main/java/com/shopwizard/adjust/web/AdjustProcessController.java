package com.shopwizard.adjust.web;

import com.shopwizard.adjust.model.AdjustProcessRequests.BatchResult;
import com.shopwizard.adjust.model.AdjustProcessRequests.CloseRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.CreateRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.PriceRequest;
import com.shopwizard.adjust.model.AdjustTarget;
import com.shopwizard.adjust.service.AdjustProcessService;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 정산 생성·마감(공급사 매입 정산). 흐름: 정산 대상 조회 → 정산자료 생성 → (금액 수정 / 생성 취소) → 상점 마감.
 * 마감 결과는 기존 "정산 관리"(tAdjAdjust)에 나타난다.
 *
 * <p>쓰기는 상점·채널로 제한되지 않은 관리자만 할 수 있고(여러 상점에 걸친 업무), 서비스코드와 처리자는 클라이언트 값이 아니라
 * 로그인 세션 값을 쓴다. 조회는 기존 데이터 범위 필터(pShopCode/pChnlCode 덮어쓰기)가 그대로 적용된다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/adjust/process")
public class AdjustProcessController {

    private final AdjustProcessService service;
    private final DataScopeGuard scopeGuard;

    // ── 정산 대상 조회 ──
    @GetMapping("/target/list")
    public List<AdjustTarget> targetList(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return service.selectTargetList(withSession(params, request));
    }

    @GetMapping("/target/count")
    public int targetCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return service.selectTargetCount(withSession(params, request));
    }

    /** 구분(SHIP/RETURN)별 건수·금액 합계. */
    @GetMapping("/target/sum")
    public List<Map<String, Object>> targetSum(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return service.selectTargetSum(withSession(params, request));
    }

    /** 상점별 아직 정산자료로 만들지 않은 대상 건수(마감 전 빠진 건 확인용). */
    @GetMapping("/target/open-count")
    public List<Map<String, Object>> openCount(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return service.selectOpenTargetCountByShop(withSession(params, request));
    }

    // ── 쓰기 ──
    @PostMapping("/create")
    public BatchResult create(@RequestBody CreateRequest req, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        return service.create(req, loginId(request), name(request));
    }

    @PutMapping("/prices")
    public void prices(@RequestBody PriceRequest req, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        if (!service.updatePrices(req, loginId(request), name(request))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 마감됐거나 없는 정산자료입니다. 새로고침 후 확인하세요.");
        }
    }

    /** 마감 전 정산자료 생성 취소. Api.delete() 는 쿼리스트링이라 번호는 경로로 받는다. */
    @DeleteMapping("/detail/{adjustDetailNo}")
    public BatchResult cancel(@PathVariable Integer adjustDetailNo, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        return service.cancel(List.of(adjustDetailNo), loginId(request), name(request));
    }

    @PostMapping("/close")
    public BatchResult close(@RequestBody CloseRequest req, HttpServletRequest request) {
        scopeGuard.requireUnscoped(request);
        return service.close(req, svcCode(request), loginId(request), name(request));
    }

    // ── 세션 값 ──
    private static Map<String, String> me(HttpServletRequest request) {
        Map<String, String> m = AuthSession.get(request, AuthSession.Type.MNGR);
        if (m == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return m;
    }

    private static String loginId(HttpServletRequest request) { return me(request).get("loginId"); }
    private static String name(HttpServletRequest request) { return me(request).get("name"); }
    private static String svcCode(HttpServletRequest request) { return me(request).get("svcCode"); }

    /** 서비스코드는 세션 값으로 고정한다(비어 있으면 클라이언트 값 그대로 — 기존 화면들과 같은 규칙). */
    private static Map<String, Object> withSession(Map<String, Object> params, HttpServletRequest request) {
        String svc = svcCode(request);
        if (svc != null && !svc.isBlank()) params.put("pSvcCode", svc);
        return params;
    }
}
