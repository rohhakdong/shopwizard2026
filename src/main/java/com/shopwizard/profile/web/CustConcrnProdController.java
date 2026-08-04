package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustConcrnProd;
import com.shopwizard.profile.model.CustConcrnProdReport;
import com.shopwizard.profile.service.CustConcrnProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custConcrnProd")
public class CustConcrnProdController {

    private final CustConcrnProdService custConcrnProdService;

    @GetMapping("/list")
    public List<CustConcrnProd> selectList(@RequestParam Map<String, Object> params) { return custConcrnProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custConcrnProdService.selectCount(params); }

    @GetMapping("/listReport")
    public List<CustConcrnProdReport> selectListReport(@RequestParam Map<String, Object> params) { return custConcrnProdService.selectListReport(params); }

    @GetMapping("/countReport")
    public int selectCountReport(@RequestParam Map<String, Object> params) { return custConcrnProdService.selectCountReport(params); }

    @GetMapping
    public CustConcrnProd select(@RequestParam Map<String, Object> params) { return custConcrnProdService.select(params); }

    @PostMapping
    public int insert(@RequestBody CustConcrnProd custConcrnProd) { return custConcrnProdService.insert(custConcrnProd); }

    @PutMapping
    public int update(@RequestBody CustConcrnProd custConcrnProd) { return custConcrnProdService.update(custConcrnProd); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return custConcrnProdService.delete(params); }

    @DeleteMapping("/byCustId/{custId}")
    public int deleteByCustId(@PathVariable Integer custId) { return custConcrnProdService.deleteByCustId(custId); }
}
