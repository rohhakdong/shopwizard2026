package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.ship.model.ReturnDirect;
import com.shopwizard.ship.model.ReturnRequestBatchRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import com.shopwizard.ship.service.ReturnDirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/return-direct")
@RequiredArgsConstructor
public class ReturnDirectController {
    private final ReturnDirectService returnDirectService;
    private final DataScopeGuard scopeGuard;   // 상점/채널 제한 관리자는 자기 라인만(조회 목록은 DataScopeFilter)
    @GetMapping("/list") public List<ReturnDirect> selectList(@RequestParam Map<String, Object> params) { return returnDirectService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return returnDirectService.selectCount(params); }
    @GetMapping public ReturnDirect select(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        ReturnDirect row = returnDirectService.select(params);
        if (row != null) scopeGuard.requireRow(request, row, row.getShopCode(), row.getChnlCode());
        return row;
    }
    // 반품 행을 만들고 지우는 쓰기(POST, POST /return-direct, DELETE, DELETE /refund-cancel)는 HTTP 로 열어두지 않는다.
    // 반품요청은 아래 request-batch 게이트(배송완료 → 반품요청, 사유 기록·상태 전환 순서 보장)로만 만든다.
    /** 반품요청 화면(ship-return-request.js) — 선택한 배송완료 주문라인들을 일괄 반품요청 처리. */
    @PostMapping("/request-batch") public ShipDirectIssueResult requestBatch(@RequestBody ReturnRequestBatchRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), ReturnRequestBatchRequest.Line::getOrderNo, ReturnRequestBatchRequest.Line::getOrderProdNo);
        return returnDirectService.requestBatch(req);
    }
    /** 반품확정 화면(ship-return.js) — 반품요청 건을 반품완료로 전환(상태는 서버가 고정, 반품요청 건만 성공). */
    @PutMapping public int update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        scopeGuard.requireLine(request, toInt(params.get("orderNo")), toInt(params.get("orderProdNo")));
        return returnDirectService.update(params);
    }
    private static Integer toInt(Object v) {
        try { return v == null ? null : Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
    }
}
