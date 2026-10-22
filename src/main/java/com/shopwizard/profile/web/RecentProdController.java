package com.shopwizard.profile.web;

import com.shopwizard.profile.model.RecentProd;
import com.shopwizard.profile.service.RecentProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/recentProd")
public class RecentProdController {

    private final RecentProdService recentProdService;

    @GetMapping("/list")
    public List<RecentProd> selectList(@RequestParam Map<String, Object> params) { return recentProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return recentProdService.selectCount(params); }

    @GetMapping
    public RecentProd select(@RequestParam Map<String, Object> params) { return recentProdService.select(params); }

}
