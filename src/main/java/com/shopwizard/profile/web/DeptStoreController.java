package com.shopwizard.profile.web;

import com.shopwizard.profile.model.DeptStore;
import com.shopwizard.profile.service.DeptStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/deptStore")
public class DeptStoreController {

    private final DeptStoreService deptStoreService;

    @GetMapping("/list")
    public List<DeptStore> selectList(@RequestParam Map<String, Object> params) { return deptStoreService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return deptStoreService.selectCount(params); }

    @GetMapping
    public DeptStore select(@RequestParam Map<String, Object> params) { return deptStoreService.select(params); }

    @PostMapping
    public int insert(@RequestBody DeptStore deptStore) { return deptStoreService.insert(deptStore); }

    @PutMapping
    public int update(@RequestBody DeptStore deptStore) { return deptStoreService.update(deptStore); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return deptStoreService.delete(params); }
}
