package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustCpnDetail;
import com.shopwizard.profile.service.CustCpnDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custCpnDetail")
public class CustCpnDetailController {

    private final CustCpnDetailService custCpnDetailService;

    @GetMapping("/list")
    public List<CustCpnDetail> selectList(@RequestParam Map<String, Object> params) { return custCpnDetailService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custCpnDetailService.selectCount(params); }

    @GetMapping
    public CustCpnDetail select(@RequestParam Map<String, Object> params) { return custCpnDetailService.select(params); }

    @PostMapping
    public int insert(@RequestBody CustCpnDetail custCpnDetail) { return custCpnDetailService.insert(custCpnDetail); }

    @PutMapping
    public int update(@RequestBody CustCpnDetail custCpnDetail) { return custCpnDetailService.update(custCpnDetail); }

    @PutMapping("/disable")
    public int disable(@RequestBody Map<String, Object> params) { return custCpnDetailService.disable(params); }

    @PutMapping("/enable")
    public int enable(@RequestBody Map<String, Object> params) { return custCpnDetailService.enable(params); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return custCpnDetailService.delete(params); }
}
