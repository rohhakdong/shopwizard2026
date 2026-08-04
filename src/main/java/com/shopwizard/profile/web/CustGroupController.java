package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustGroup;
import com.shopwizard.profile.service.CustGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custGroup")
public class CustGroupController {

    private final CustGroupService custGroupService;

    @GetMapping("/list")
    public List<CustGroup> selectList(@RequestParam Map<String, Object> params) { return custGroupService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custGroupService.selectCount(params); }

    @GetMapping("/{custGroupCode}")
    public CustGroup select(@PathVariable String custGroupCode) { return custGroupService.select(custGroupCode); }

    @PostMapping
    public int insert(@RequestBody CustGroup custGroup) { return custGroupService.insert(custGroup); }

    @PutMapping
    public int update(@RequestBody CustGroup custGroup) { return custGroupService.update(custGroup); }

    @DeleteMapping("/{custGroupCode}")
    public int delete(@PathVariable String custGroupCode) { return custGroupService.delete(custGroupCode); }
}
