package com.shopwizard.ship.web;

import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.model.ShipDirect;
import com.shopwizard.ship.service.GiftOptionUpdateDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/gift-option-update-detail")
@RequiredArgsConstructor
public class GiftOptionUpdateDetailController {
    private final GiftOptionUpdateDetailService giftOptionUpdateDetailService;
    @GetMapping("/list") public List<GiftOptionUpdateDetail> selectList(@RequestParam Map<String, Object> params) { return giftOptionUpdateDetailService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return giftOptionUpdateDetailService.selectCount(params); }
    @GetMapping public GiftOptionUpdateDetail select(@RequestParam Map<String, Object> params) { return giftOptionUpdateDetailService.select(params); }
    @PostMapping public int insert(@RequestBody GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailService.insert(giftOptionUpdateDetail); }
    @PutMapping public int update(@RequestBody GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailService.update(giftOptionUpdateDetail); }
    @DeleteMapping public int delete(@RequestBody GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailService.delete(giftOptionUpdateDetail); }
    @GetMapping("/ship-direct-list") public List<ShipDirect> selectShipDirectList(@RequestParam Map<String, Object> params) { return giftOptionUpdateDetailService.selectShipDirectList(params); }
    @GetMapping("/ship-direct-print-list") public List<ShipDirect> selectShipDirectPrintList(@RequestParam Map<String, Object> params) { return giftOptionUpdateDetailService.selectShipDirectPrintList(params); }
    @PutMapping("/ship-direct-print") public int updateShipDirectPrint(@RequestBody Map<String, Object> params) { return giftOptionUpdateDetailService.updateShipDirectPrint(params); }
    @PutMapping("/ship-direct-print-autionno") public int updateShipDirectPrintAutionno(@RequestBody Map<String, Object> params) { return giftOptionUpdateDetailService.updateShipDirectPrintAutionno(params); }
}
