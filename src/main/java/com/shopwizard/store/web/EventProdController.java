package com.shopwizard.store.web;

import com.shopwizard.store.model.EventProd;
import com.shopwizard.store.service.EventProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/eventprod")
@RequiredArgsConstructor
public class EventProdController {
    private final EventProdService eventProdService;

    @GetMapping("/list")
    public List<EventProd> selectList(@RequestParam Map<String, Object> params) {
        return eventProdService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return eventProdService.selectCount(params);
    }

    @GetMapping("/all/list")
    public List<EventProd> selectListAll(@RequestParam Map<String, Object> params) {
        return eventProdService.selectListAll(params);
    }

}
