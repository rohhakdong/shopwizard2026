package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustApprovLine;
import com.shopwizard.profile.service.CustApprovLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custApprovLine")
public class CustApprovLineController {

    private final CustApprovLineService custApprovLineService;

    @GetMapping("/list")
    public List<CustApprovLine> selectList(@RequestParam Map<String, Object> params) { return custApprovLineService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custApprovLineService.selectCount(params); }

    @GetMapping
    public CustApprovLine select(@RequestParam Map<String, Object> params) { return custApprovLineService.select(params); }

    @GetMapping("/approvLevelMax/{custId}")
    public Integer selectApprovLevelMax(@PathVariable Integer custId) { return custApprovLineService.selectApprovLevelMax(custId); }

    @PostMapping
    public int insert(@RequestBody CustApprovLine custApprovLine) { return custApprovLineService.insert(custApprovLine); }

    @PutMapping
    public int update(@RequestBody CustApprovLine custApprovLine) { return custApprovLineService.update(custApprovLine); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return custApprovLineService.delete(params); }
}
