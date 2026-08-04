package com.shopwizard.profile.web;

import com.shopwizard.profile.model.DeptEmp;
import com.shopwizard.profile.service.DeptEmpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/deptEmp")
public class DeptEmpController {

    private final DeptEmpService deptEmpService;

    @GetMapping("/list")
    public List<DeptEmp> selectList(@RequestParam Map<String, Object> params) { return deptEmpService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return deptEmpService.selectCount(params); }

    @GetMapping
    public DeptEmp select(@RequestParam Map<String, Object> params) { return deptEmpService.select(params); }

    @PostMapping
    public int insert(@RequestBody DeptEmp deptEmp) { return deptEmpService.insert(deptEmp); }

    @PutMapping
    public int update(@RequestBody DeptEmp deptEmp) { return deptEmpService.update(deptEmp); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return deptEmpService.delete(params); }
}
