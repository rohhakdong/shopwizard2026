package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CompStore;
import com.shopwizard.profile.service.CompStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/compStore")
public class CompStoreController {

    private final CompStoreService compStoreService;

    @GetMapping("/list")
    public List<CompStore> selectList(@RequestParam Map<String, Object> params) { return compStoreService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return compStoreService.selectCount(params); }

    @GetMapping("/listFinalTable")
    public List<CompStore> selectListFinalTable(@RequestParam Map<String, Object> params) { return compStoreService.selectListFinalTable(params); }

    @GetMapping("/listLevel")
    public List<CompStore> selectListLevel(@RequestParam Map<String, Object> params) { return compStoreService.selectListLevel(params); }

    @GetMapping("/storeCodeMax")
    public String selectStoreCodeMax(@RequestParam Map<String, Object> params) { return compStoreService.selectStoreCodeMax(params); }

    @GetMapping
    public CompStore select(@RequestParam Map<String, Object> params) { return compStoreService.select(params); }

    @PostMapping
    public int insert(@RequestBody CompStore compStore) { return compStoreService.insert(compStore); }

    @PutMapping
    public int update(@RequestBody CompStore compStore) { return compStoreService.update(compStore); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return compStoreService.delete(params); }
}
