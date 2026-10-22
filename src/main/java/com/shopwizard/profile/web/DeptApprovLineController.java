package com.shopwizard.profile.web;

import com.shopwizard.profile.model.DeptApprovLine;
import com.shopwizard.profile.service.DeptApprovLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/deptApprovLine")
public class DeptApprovLineController {

    private final DeptApprovLineService deptApprovLineService;

    @GetMapping("/list")
    public List<DeptApprovLine> selectList(@RequestParam Map<String, Object> params) { return deptApprovLineService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return deptApprovLineService.selectCount(params); }

    @GetMapping
    public DeptApprovLine select(@RequestParam Map<String, Object> params) { return deptApprovLineService.select(params); }

}
