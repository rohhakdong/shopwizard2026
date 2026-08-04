package com.shopwizard.company.web;

import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.company.service.SupplyCompNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/supply-comp-notice")
@RequiredArgsConstructor
public class SupplyCompNoticeController {
    private final SupplyCompNoticeService supplyCompNoticeService;

    @GetMapping
    public List<SupplyCompNotice> getList(@RequestParam Map<String, Object> params) {
        return supplyCompNoticeService.getList(params);
    }

    @GetMapping("/{noticeNo}")
    public SupplyCompNotice get(@PathVariable int noticeNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("noticeNo", noticeNo);
        return supplyCompNoticeService.get(params);
    }

    @GetMapping("/{noticeNo}/cntnts")
    public String getCntnts(@PathVariable int noticeNo) {
        return supplyCompNoticeService.getCntnts(noticeNo);
    }

    @PostMapping
    public int insert(@RequestBody SupplyCompNotice supplyCompNotice) {
        return supplyCompNoticeService.insert(supplyCompNotice);
    }

    @PutMapping
    public int update(@RequestBody SupplyCompNotice supplyCompNotice) {
        return supplyCompNoticeService.update(supplyCompNotice);
    }

    @DeleteMapping
    public int delete(@RequestBody SupplyCompNotice supplyCompNotice) {
        return supplyCompNoticeService.delete(supplyCompNotice);
    }
}
