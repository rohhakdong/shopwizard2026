package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustInfoLog;
import com.shopwizard.profile.service.CustInfoLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custInfoLog")
public class CustInfoLogController {

    private final CustInfoLogService custInfoLogService;

    @GetMapping("/list")
    public List<CustInfoLog> selectList(@RequestParam Map<String, Object> params) { return custInfoLogService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custInfoLogService.selectCount(params); }

    @GetMapping("/{logId}")
    public CustInfoLog select(@PathVariable Integer logId) { return custInfoLogService.select(logId); }

    @PostMapping
    public int insert(@RequestBody CustInfoLog custInfoLog) { return custInfoLogService.insert(custInfoLog); }

    @PutMapping
    public int update(@RequestBody CustInfoLog custInfoLog) { return custInfoLogService.update(custInfoLog); }

    @DeleteMapping("/{logId}")
    public int delete(@PathVariable Integer logId) { return custInfoLogService.delete(logId); }
}
