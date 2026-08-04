package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Dept;
import com.shopwizard.profile.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/dept")
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/list")
    public List<Dept> selectList(@RequestParam Map<String, Object> params) { return deptService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return deptService.selectCount(params); }

    @GetMapping
    public Dept select(@RequestParam Map<String, Object> params) { return deptService.select(params); }

    @GetMapping("/deptCodeMax")
    public Map<String, Object> selectDeptCodeMax(@RequestParam Map<String, Object> params) { return deptService.selectDeptCodeMax(params); }

    @PostMapping
    public int insert(@RequestBody Dept dept) { return deptService.insert(dept); }

    @PutMapping
    public int update(@RequestBody Dept dept) { return deptService.update(dept); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return deptService.delete(params); }
}
