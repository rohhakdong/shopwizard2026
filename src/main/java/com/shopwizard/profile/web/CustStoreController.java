package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustStore;
import com.shopwizard.profile.service.CustStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custStore")
public class CustStoreController {

    private final CustStoreService custStoreService;

    @GetMapping("/list")
    public List<CustStore> selectList(@RequestParam Map<String, Object> params) { return custStoreService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custStoreService.selectCount(params); }

    @GetMapping
    public CustStore select(@RequestParam Map<String, Object> params) { return custStoreService.select(params); }

}
