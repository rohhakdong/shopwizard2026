package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Cate;
import com.shopwizard.catalog.service.CateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/cate")
@RequiredArgsConstructor
public class CateController {
    private final CateService cateService;

    @GetMapping("/list") public List<Cate> selectList(@RequestParam Map<String, Object> params) { return cateService.selectList(params); }
    @GetMapping("/code-max") public String selectCateCodeMax(@RequestParam Map<String, Object> params) { return cateService.selectCateCodeMax(params); }
    @GetMapping public Cate select(@RequestParam Map<String, Object> params) { return cateService.select(params); }
    @GetMapping("/list-level") public List<Cate> selectListLevel(@RequestParam Map<String, Object> params) { return cateService.selectListLevel(params); }
    @GetMapping("/list-final-table") public List<Cate> selectListFinalTable(@RequestParam Map<String, Object> params) { return cateService.selectListFinalTable(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return cateService.selectCount(params); }
    @GetMapping("/count-child") public int selectCountChild(@RequestParam Map<String, Object> params) { return cateService.selectCountChild(params); }
    @PostMapping public int insert(@RequestBody Cate cate) { return cateService.insert(cate); }
    @PutMapping public int update(@RequestBody Cate cate) { return cateService.update(cate); }
    @DeleteMapping public int delete(@RequestBody Cate cate) { return cateService.delete(cate); }
}
