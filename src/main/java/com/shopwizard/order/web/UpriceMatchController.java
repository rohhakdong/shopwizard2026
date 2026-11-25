package com.shopwizard.order.web;

import com.shopwizard.order.model.UpriceMatchDetail;
import com.shopwizard.order.model.UpriceMatchSaveRequest;
import com.shopwizard.order.service.UpriceMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발주단가매칭 (order-uprice-match.js). 채널 상품명을 발주단가코드(1개 이상)로 분해해 저장한다.
 * 저장된 규칙이 없으면 출고지시가 막힌다 — 가드는 ShipDirectIssueService 쪽에 있다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/uprice-match")
public class UpriceMatchController {

    private final UpriceMatchService upriceMatchService;

    @GetMapping("/unmatched/list")
    public List<Map<String, Object>> unmatchedList(@RequestParam Map<String, Object> params) {
        return upriceMatchService.selectUnmatched(params);
    }

    @GetMapping("/unmatched/list/count")
    public int unmatchedCount(@RequestParam Map<String, Object> params) {
        return upriceMatchService.selectUnmatchedCount(params);
    }

    @GetMapping("/detail")
    public List<UpriceMatchDetail> detail(@RequestParam String prodName, @RequestParam String itemName) {
        return upriceMatchService.selectDetail(prodName, itemName);
    }

    // ── 단가매칭결과 화면(order-uprice-match-result): 규칙 목록·줄 조회·삭제. 등록·수정은 아래 /save 를 같이 쓴다. ──

    @GetMapping("/list")
    public List<Map<String, Object>> rules(@RequestParam Map<String, Object> params) {
        return upriceMatchService.selectRules(params);
    }

    @GetMapping("/count")
    public int ruleCount(@RequestParam Map<String, Object> params) {
        return upriceMatchService.selectRuleCount(params);
    }

    /** 규칙의 줄 — 사용 중지·삭제된 코드도 나온다(/detail 은 사용 중인 코드만). */
    @GetMapping("/lines")
    public List<UpriceMatchDetail> lines(@RequestParam String prodName, @RequestParam(defaultValue = "") String itemName) {
        return upriceMatchService.selectRuleLines(prodName, itemName);
    }

    @DeleteMapping
    public void delete(@RequestParam String prodName, @RequestParam(defaultValue = "") String itemName) {
        upriceMatchService.deleteRule(prodName, itemName);
    }

    @PostMapping("/save")
    public void save(@RequestBody UpriceMatchSaveRequest req) {
        upriceMatchService.saveMatch(req.getProdName(), req.getItemName(), req.getLines(),
                req.getRegistId(), req.getRegistName());
    }
}
