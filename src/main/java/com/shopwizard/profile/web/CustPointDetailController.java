package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustPointDetail;
import com.shopwizard.profile.service.CustPointDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custPointDetail")
public class CustPointDetailController {

    private final CustPointDetailService custPointDetailService;

    @GetMapping("/list")
    public List<CustPointDetail> selectList(@RequestParam Map<String, Object> params) { return custPointDetailService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custPointDetailService.selectCount(params); }

    @GetMapping
    public CustPointDetail select(@RequestParam Map<String, Object> params) { return custPointDetailService.select(params); }

    @GetMapping("/sum")
    public Integer selectSum(@RequestParam Map<String, Object> params) { return custPointDetailService.selectSum(params); }

}
