package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Estimt;
import com.shopwizard.profile.service.EstimtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/estimt")
public class EstimtController {

    private final EstimtService estimtService;

    @GetMapping("/list")
    public List<Estimt> selectList(@RequestParam Map<String, Object> params) { return estimtService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return estimtService.selectCount(params); }

    @GetMapping
    public Estimt select(@RequestParam Map<String, Object> params) { return estimtService.select(params); }

    @GetMapping("/listStatDept")
    public List<Estimt> selectListStatDept(@RequestParam Map<String, Object> params) { return estimtService.selectListStatDept(params); }

    @GetMapping("/monthlyTotalAmtDeptAccnt")
    public Double selectMonthlyTotalAmtDeptAccnt(@RequestParam Map<String, Object> params) { return estimtService.selectMonthlyTotalAmtDeptAccnt(params); }

    @PostMapping
    public int insert(@RequestBody Estimt estimt) { return estimtService.insert(estimt); }

    @PutMapping
    public int update(@RequestBody Estimt estimt) { return estimtService.update(estimt); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return estimtService.delete(params); }
}
