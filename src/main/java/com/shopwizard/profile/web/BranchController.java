package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Branch;
import com.shopwizard.profile.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/branch")
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/list")
    public List<Branch> selectList(@RequestParam Map<String, Object> params) { return branchService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return branchService.selectCount(params); }

    @GetMapping("/{branchId}")
    public Branch select(@PathVariable Integer branchId) { return branchService.select(branchId); }

    @PostMapping
    public int insert(@RequestBody Branch branch) { return branchService.insert(branch); }

    @PutMapping
    public int update(@RequestBody Branch branch) { return branchService.update(branch); }

    @DeleteMapping("/{branchId}")
    public int delete(@PathVariable Integer branchId) { return branchService.delete(branchId); }
}
