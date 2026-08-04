package com.shopwizard.ship.web;

import com.shopwizard.ship.model.GiftOptionUpdate;
import com.shopwizard.ship.service.GiftOptionUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/gift-option-update")
@RequiredArgsConstructor
public class GiftOptionUpdateController {
    private final GiftOptionUpdateService giftOptionUpdateService;
    @GetMapping("/list") public List<GiftOptionUpdate> selectList(@RequestParam Map<String, Object> params) { return giftOptionUpdateService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return giftOptionUpdateService.selectCount(params); }
    @GetMapping public GiftOptionUpdate select(@RequestParam Map<String, Object> params) { return giftOptionUpdateService.select(params); }
    @PostMapping public int insert(@RequestBody GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateService.insert(giftOptionUpdate); }
    @PutMapping public int update(@RequestBody GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateService.update(giftOptionUpdate); }
    @DeleteMapping public int delete(@RequestBody GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateService.delete(giftOptionUpdate); }
}
