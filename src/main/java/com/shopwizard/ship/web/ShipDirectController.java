package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.ship.model.ShipDirect;
import com.shopwizard.ship.model.ShipDirectCheckRequest;
import com.shopwizard.ship.model.ShipDirectIssueRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import com.shopwizard.ship.service.ShipDirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/ship-direct")
@RequiredArgsConstructor
public class ShipDirectController {
    private final ShipDirectService shipDirectService;
    // 상점/채널로 제한된 관리자는 자기 라인만 다룬다(DataScopeGuard) — 목록은 DataScopeFilter 가 pShopCode/pChnlCode 로 거른다.
    private final DataScopeGuard scopeGuard;
    @GetMapping("/list") public List<ShipDirect> selectList(@RequestParam Map<String, Object> params) { return shipDirectService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return shipDirectService.selectCount(params); }
    @GetMapping public ShipDirect select(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        ShipDirect row = shipDirectService.select(params);
        if (row != null) scopeGuard.requireRow(request, row, row.getShopCode(), row.getChnlCode());
        return row;
    }
    @GetMapping("/simple") public ShipDirect selectSimple(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        ShipDirect row = shipDirectService.selectSimple(params);
        if (row != null) scopeGuard.requireRow(request, row, row.getShopCode(), row.getChnlCode());
        return row;
    }
    @GetMapping("/ship-place-list") public List<ShipDirect> selectListShipPlace(@RequestParam Map<String, Object> params) { return shipDirectService.selectListShipPlace(params); }
    @GetMapping("/print2-list") public List<ShipDirect> selectListPrint2(@RequestParam Map<String, Object> params) { return shipDirectService.selectListPrint2(params); }
    @GetMapping("/print2-count") public int selectCountPrint2(@RequestParam Map<String, Object> params) { return shipDirectService.selectCountPrint2(params); }
    // 출고/발주/배송 상태를 바꾸는 쓰기는 아래 배치 게이트(issue/check/complete-batch)와 송장입력(PUT /invoice)뿐이다.
    // 임의 행 생성(POST, POST /ship-direct)·임의 상태 변경(PUT)·환불취소(PUT /refund-cancel)·삭제(DELETE)는
    // 상태 가드·재고 이력을 건너뛸 수 있어 HTTP 로 열어두지 않는다(서버 내부는 ShipDirectService 를 직접 호출).
    /** 출고지시 화면(ship-direct-issue.js) — 선택한 지불완료 주문라인들을 일괄 출고지시 처리. */
    @PostMapping("/issue-batch") public ShipDirectIssueResult issueBatch(@RequestBody ShipDirectIssueRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), ShipDirectIssueRequest.Line::getOrderNo, ShipDirectIssueRequest.Line::getOrderProdNo);
        return shipDirectService.issueBatch(req);
    }
    /** 발주확인 화면(ship-direct-check.js) — 선택한 출고지시 건들을 일괄 발주확인 처리. */
    @PostMapping("/check-batch") public ShipDirectIssueResult checkBatch(@RequestBody ShipDirectCheckRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), ShipDirectCheckRequest.Line::getOrderNo, ShipDirectCheckRequest.Line::getOrderProdNo);
        return shipDirectService.checkBatch(req);
    }
    /** 배송완료 화면(ship-direct-complete.js) — 선택한 배송시작 건들을 일괄 배송완료 처리. */
    @PostMapping("/complete-batch") public ShipDirectIssueResult completeBatch(@RequestBody ShipDirectCheckRequest req, HttpServletRequest request) {
        scopeGuard.requireLines(request, req.getLines(), ShipDirectCheckRequest.Line::getOrderNo, ShipDirectCheckRequest.Line::getOrderProdNo);
        return shipDirectService.completeBatch(req);
    }
    @GetMapping("/social-check") public int checkShipDirectPrintByMatchingSocial(@RequestParam Map<String, Object> params) { return shipDirectService.checkShipDirectPrintByMatchingSocial(params); }
    /** 송장입력 화면(ship-direct.js) — 발주확인 건에 택배사/송장번호를 입력하고 배송시작으로 전환(상태는 서버가 고정). */
    @PutMapping("/invoice") public int updateInvoice(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        scopeGuard.requireLine(request, toInt(params.get("orderNo")), toInt(params.get("orderProdNo")));
        return shipDirectService.updateInvoice(params);
    }
    private static Integer toInt(Object v) {
        try { return v == null ? null : Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
    }
    @GetMapping("/invoice-count") public int countInvoice(@RequestParam Map<String, Object> params) { return shipDirectService.countInvoice(params); }
    @GetMapping("/cmplet-list") public List<ShipDirect> selectCmpletList(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletList(params); }
    @GetMapping("/cmplet-count") public int selectCmpletCount(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletCount(params); }
    @GetMapping("/cmplet-list-by-order") public List<ShipDirect> selectCmpletListByOrder(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletListByOrder(params); }
    @GetMapping("/cmplet-count-by-order") public int selectCmpletCountByOrder(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletCountByOrder(params); }
}
