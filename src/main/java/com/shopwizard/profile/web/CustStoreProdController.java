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

    @PostMapping
    public int insert(@RequestBody CustStoreProd custStoreProd) { return custStoreProdService.insert(custStoreProd); }

    @PutMapping
    public int update(@RequestBody CustStoreProd custStoreProd) { return custStoreProdService.update(custStoreProd); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return custStoreProdService.delete(params); }
}
