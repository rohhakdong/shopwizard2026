package com.shopwizard.ship.web;

import com.shopwizard.ship.model.ReturnDirect;
import com.shopwizard.ship.model.ReturnRequestBatchRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import com.shopwizard.ship.service.ReturnDirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/return-direct")
@RequiredArgsConstructor
public class ReturnDirectController {
    private final ReturnDirectService returnDirectService;
    @GetMapping("/list") public List<ReturnDirect> selectList(@RequestParam Map<String, Object> params) { return returnDirectService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return returnDirectService.selectCount(params); }
    @GetMapping public ReturnDirect select(@RequestParam Map<String, Object> params) { return returnDirectService.select(params); }
    @PostMapping public int insert(@RequestBody ReturnDirect returnDirect) { return returnDirectService.insert(returnDirect); }
    @PostMapping("/return-direct") public int insertReturnDirect(@RequestBody Map<String, Object> params) { return returnDirectService.insertReturnDirect(params); }
    /** 반품요청 화면(ship-return-request.js) — 선택한 배송완료 주문라인들을 일괄 반품요청 처리. */
    @PostMapping("/request-batch") public ShipDirectIssueResult requestBatch(@RequestBody ReturnRequestBatchRequest req) { return returnDirectService.requestBatch(req); }
    @PutMapping public int update(@RequestBody Map<String, Object> params) { return returnDirectService.update(params); }
    @PutMapping("/adjust-select-date") public int updateAdjustSelectDate(@RequestBody Map<String, Object> params) { return returnDirectService.updateAdjustSelectDate(params); }
    @PutMapping("/adjust-select-date-schedule") public int updateAdjustSelectDateSchedule(@RequestBody Map<String, Object> params) { return returnDirectService.updateAdjustSelectDateSchedule(params); }
    @DeleteMapping public int delete(@RequestBody ReturnDirect returnDirect) { return returnDirectService.delete(returnDirect); }
    @DeleteMapping("/refund-cancel") public int deleteRefundCancel(@RequestBody Map<String, Object> params) { return returnDirectService.deleteRefundCancel(params); }
}
