package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdReview;
import com.shopwizard.product.service.ProdReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prodreview")
public class ProdReviewController {
    private final ProdReviewService prodReviewService;

    @GetMapping("/list")
    public List<ProdReview> selectList(@RequestParam Map<String, Object> params) { return prodReviewService.selectList(params); }

    @GetMapping("/{reviewNo}")
    public ProdReview select(@PathVariable Integer reviewNo) { return prodReviewService.select(reviewNo); }

    @PostMapping
    public void insert(@RequestBody ProdReview prodReview) { prodReviewService.insert(prodReview); }

    @PutMapping
    public void update(@RequestBody ProdReview prodReview) { prodReviewService.update(prodReview); }

    @DeleteMapping("/{reviewNo}")
    public void delete(@PathVariable Integer reviewNo) { prodReviewService.delete(reviewNo); }

    @GetMapping("/best/list")
    public List<ProdReview> selectListBest(@RequestParam Map<String, Object> params) { return prodReviewService.selectListBest(params); }

    @GetMapping("/count")
    public int selectListCount(@RequestParam Map<String, Object> params) { return prodReviewService.selectListCount(params); }

    @GetMapping("/page/list")
    public List<ProdReview> selectListPage(@RequestParam Map<String, Object> params) { return prodReviewService.selectListPage(params); }

    @GetMapping("/byprodcode/list")
    public List<ProdReview> selectListByProdCode(@RequestParam Map<String, Object> params) { return prodReviewService.selectListByProdCode(params); }

    @GetMapping("/bybrandname/list")
    public List<ProdReview> selectListByBrandName(@RequestParam Map<String, Object> params) { return prodReviewService.selectListByBrandName(params); }
}
