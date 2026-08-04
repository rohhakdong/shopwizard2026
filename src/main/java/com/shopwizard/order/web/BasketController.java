package com.shopwizard.order.web;

import com.shopwizard.order.model.Basket;
import com.shopwizard.order.service.BasketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/basket")
public class BasketController {
    private final BasketService basketService;

    @GetMapping("/full/list")
    public List<Basket> selectListFull(@RequestParam Map<String, Object> params) { return basketService.selectListFull(params); }

    @GetMapping("/direct")
    public Basket selectDirect(@RequestParam Map<String, Object> params) { return basketService.selectDirect(params); }

    @GetMapping("/list")
    public List<Basket> selectList(@RequestParam Map<String, Object> params) { return basketService.selectList(params); }

    @GetMapping
    public Basket select(@RequestParam Map<String, Object> params) { return basketService.select(params); }

    @GetMapping("/item")
    public Basket selectByItem(@RequestParam Map<String, Object> params) { return basketService.selectByItem(params); }

    @PostMapping
    public void insert(@RequestBody Basket basket) { basketService.insert(basket); }

    @PutMapping
    public void update(@RequestBody Map<String, Object> params) { basketService.update(params); }

    @PutMapping("/custid")
    public void updateCustId(@RequestBody Map<String, Object> params) { basketService.updateCustId(params); }

    @PutMapping("/mro")
    public void updateMro(@RequestBody Map<String, Object> params) { basketService.updateMro(params); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { basketService.delete(params); }

    @DeleteMapping("/multi")
    public void deleteMulti(@RequestBody Map<String, Object> params) { basketService.deleteMulti(params); }

    @DeleteMapping("/clean")
    public void clean(@RequestBody Map<String, Object> params) { basketService.clean(params); }
}
