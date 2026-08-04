package com.shopwizard.profile.web;

import com.shopwizard.profile.model.BranchStoreProd;
import com.shopwizard.profile.service.BranchStoreProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/branchStoreProd")
public class BranchStoreProdController {

    private final BranchStoreProdService branchStoreProdService;

    @GetMapping("/list")
    public List<BranchStoreProd> selectList(@RequestParam Map<String, Object> params) { return branchStoreProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return branchStoreProdService.selectCount(params); }

    @GetMapping
    public BranchStoreProd select(@RequestParam Map<String, Object> params) { return branchStoreProdService.select(params); }

    @PostMapping
    public int insert(@RequestBody BranchStoreProd branchStoreProd) { return branchStoreProdService.insert(branchStoreProd); }

    @PutMapping
    public int update(@RequestBody BranchStoreProd branchStoreProd) { return branchStoreProdService.update(branchStoreProd); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return branchStoreProdService.delete(params); }
}
