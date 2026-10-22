package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Duty;
import com.shopwizard.profile.service.DutyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/duty")
public class DutyController {

    private final DutyService dutyService;

    @GetMapping("/list")
    public List<Duty> selectList(@RequestParam Map<String, Object> params) { return dutyService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return dutyService.selectCount(params); }

    @GetMapping("/{dutyId}")
    public Duty select(@PathVariable Integer dutyId) { return dutyService.select(dutyId); }

}
