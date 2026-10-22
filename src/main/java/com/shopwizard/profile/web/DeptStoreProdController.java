package com.shopwizard.profile.web;

import com.shopwizard.profile.model.DeptStoreProd;
import com.shopwizard.profile.service.DeptStoreProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/deptStoreProd")
public class DeptStoreProdController {

    private final DeptStoreProdService deptStoreProdService;

    @GetMapping("/list")
    public List<DeptStoreProd> selectList(@RequestParam Map<String, Object> params) { return deptStoreProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return deptStoreProdService.selectCount(params); }

    @GetMapping
    public DeptStoreProd select(@RequestParam Map<String, Object> params) { return deptStoreProdService.select(params); }

}
