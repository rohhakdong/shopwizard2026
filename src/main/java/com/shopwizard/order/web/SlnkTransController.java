package com.shopwizard.order.web;

import com.shopwizard.order.model.SlnkTrans;
import com.shopwizard.order.model.SlnkTransDetail;
import com.shopwizard.order.service.SlnkTransDetailService;
import com.shopwizard.order.service.SlnkTransService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 샵링커 주문수집 이력 조회 (읽기 전용).
 * 실행은 {@link ShoplinkerCollectController} (POST /order/shoplinker/collect).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/slnktrans")
public class SlnkTransController {
    private final SlnkTransService slnkTransService;
    private final SlnkTransDetailService slnkTransDetailService;

    @GetMapping("/list")
    public List<SlnkTrans> selectList(@RequestParam Map<String, Object> params) {
        return slnkTransService.selectList(params);
    }

    @GetMapping("/list/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return slnkTransService.selectCount(params);
    }

    @GetMapping("/detail/list")
    public List<SlnkTransDetail> selectDetailList(@RequestParam Map<String, Object> params) {
        return slnkTransDetailService.selectList(params);
    }
}
