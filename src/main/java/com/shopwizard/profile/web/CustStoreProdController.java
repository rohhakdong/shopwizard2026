package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustStoreProd;
import com.shopwizard.profile.service.CustStoreProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custStoreProd")
public class CustStoreProdController {

    private final CustStoreProdService custStoreProdService;

    @GetMapping("/list")
    public List<CustStoreProd> selectList(@RequestParam Map<String, Object> params) { return custStoreProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custStoreProdService.selectCount(params); }

    @GetMapping
    public CustStoreProd select(@RequestParam Map<String, Object> params) { return custStoreProdService.select(params); }

}
