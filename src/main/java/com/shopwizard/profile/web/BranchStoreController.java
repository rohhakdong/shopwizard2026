package com.shopwizard.profile.web;

import com.shopwizard.profile.model.BranchStore;
import com.shopwizard.profile.service.BranchStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/branchStore")
public class BranchStoreController {

    private final BranchStoreService branchStoreService;

    @GetMapping("/list")
    public List<BranchStore> selectList(@RequestParam Map<String, Object> params) { return branchStoreService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return branchStoreService.selectCount(params); }

    @GetMapping
    public BranchStore select(@RequestParam Map<String, Object> params) { return branchStoreService.select(params); }

    @PostMapping
    public int insert(@RequestBody BranchStore branchStore) { return branchStoreService.insert(branchStore); }

    @PutMapping
    public int update(@RequestBody BranchStore branchStore) { return branchStoreService.update(branchStore); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return branchStoreService.delete(params); }
}
