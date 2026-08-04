package com.shopwizard.ship.web;

import com.shopwizard.ship.model.ShipDirect;
import com.shopwizard.ship.service.ShipDirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/ship-direct")
@RequiredArgsConstructor
public class ShipDirectController {
    private final ShipDirectService shipDirectService;
    @GetMapping("/list") public List<ShipDirect> selectList(@RequestParam Map<String, Object> params) { return shipDirectService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return shipDirectService.selectCount(params); }
    @GetMapping public ShipDirect select(@RequestParam Map<String, Object> params) { return shipDirectService.select(params); }
    @GetMapping("/simple") public ShipDirect selectSimple(@RequestParam Map<String, Object> params) { return shipDirectService.selectSimple(params); }
    @GetMapping("/ship-place-list") public List<ShipDirect> selectListShipPlace(@RequestParam Map<String, Object> params) { return shipDirectService.selectListShipPlace(params); }
    @GetMapping("/print2-list") public List<ShipDirect> selectListPrint2(@RequestParam Map<String, Object> params) { return shipDirectService.selectListPrint2(params); }
    @GetMapping("/print2-count") public int selectCountPrint2(@RequestParam Map<String, Object> params) { return shipDirectService.selectCountPrint2(params); }
    @PostMapping public int insert(@RequestBody ShipDirect shipDirect) { return shipDirectService.insert(shipDirect); }
    @PostMapping("/ship-direct") public int insertShipDirect(@RequestBody Map<String, Object> params) { return shipDirectService.insertShipDirect(params); }
    @PostMapping("/ship-direct-print") public int insertShipDirectPrint(@RequestBody Map<String, Object> params) { return shipDirectService.insertShipDirectPrint(params); }
    @PostMapping("/ship-direct-print-by-matching") public int insertShipDirectPrintByMatching(@RequestBody Map<String, Object> params) { return shipDirectService.insertShipDirectPrintByMatching(params); }
    @GetMapping("/social-check") public int checkShipDirectPrintByMatchingSocial(@RequestParam Map<String, Object> params) { return shipDirectService.checkShipDirectPrintByMatchingSocial(params); }
    @PostMapping("/ship-direct-print-by-matching-social") public int insertShipDirectPrintByMatchingSocial(@RequestBody Map<String, Object> params) { return shipDirectService.insertShipDirectPrintByMatchingSocial(params); }
    @PostMapping("/ship-direct-print-with-gift") public int insertShipDirectPrintWithGift(@RequestBody Map<String, Object> params) { return shipDirectService.insertShipDirectPrintWithGift(params); }
    @PutMapping public int update(@RequestBody Map<String, Object> params) { return shipDirectService.update(params); }
    @PutMapping("/print") public int updatePrint(@RequestBody Map<String, Object> params) { return shipDirectService.updatePrint(params); }
    @PutMapping("/print-sort-name") public int updatePrintSortName(@RequestBody Map<String, Object> params) { return shipDirectService.updatePrintSortName(params); }
    @PutMapping("/refund-cancel") public int updateRefundCancel(@RequestBody Map<String, Object> params) { return shipDirectService.updateRefundCancel(params); }
    @PutMapping("/invoice") public int updateInvoice(@RequestBody Map<String, Object> params) { return shipDirectService.updateInvoice(params); }
    @PutMapping("/adjust-select-date") public int updateAdjustSelectDate(@RequestBody Map<String, Object> params) { return shipDirectService.updateAdjustSelectDate(params); }
    @PutMapping("/adjust-select-date-schedule") public int updateAdjustSelectDateSchedule(@RequestBody Map<String, Object> params) { return shipDirectService.updateAdjustSelectDateSchedule(params); }
    @GetMapping("/invoice-count") public int countInvoice(@RequestParam Map<String, Object> params) { return shipDirectService.countInvoice(params); }
    @DeleteMapping public int delete(@RequestBody ShipDirect shipDirect) { return shipDirectService.delete(shipDirect); }
    @GetMapping("/cmplet-list") public List<ShipDirect> selectCmpletList(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletList(params); }
    @GetMapping("/cmplet-count") public int selectCmpletCount(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletCount(params); }
    @GetMapping("/cmplet-list-by-order") public List<ShipDirect> selectCmpletListByOrder(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletListByOrder(params); }
    @GetMapping("/cmplet-count-by-order") public int selectCmpletCountByOrder(@RequestParam Map<String, Object> params) { return shipDirectService.selectCmpletCountByOrder(params); }
}
