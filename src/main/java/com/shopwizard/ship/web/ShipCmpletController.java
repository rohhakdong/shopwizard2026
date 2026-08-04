package com.shopwizard.ship.web;

import com.shopwizard.ship.model.ShipCmplet;
import com.shopwizard.ship.service.ShipCmpletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/ship-cmplet")
@RequiredArgsConstructor
public class ShipCmpletController {
    private final ShipCmpletService shipCmpletService;
    @GetMapping("/list") public List<ShipCmplet> selectList(@RequestParam Map<String, Object> params) { return shipCmpletService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return shipCmpletService.selectCount(params); }
    @GetMapping public ShipCmplet select(@RequestParam Map<String, Object> params) { return shipCmpletService.select(params); }
    @PostMapping public int insert(@RequestBody ShipCmplet shipCmplet) { return shipCmpletService.insert(shipCmplet); }
    @PutMapping public int update(@RequestBody ShipCmplet shipCmplet) { return shipCmpletService.update(shipCmplet); }
    @DeleteMapping public int delete(@RequestBody ShipCmplet shipCmplet) { return shipCmpletService.delete(shipCmplet); }
}
