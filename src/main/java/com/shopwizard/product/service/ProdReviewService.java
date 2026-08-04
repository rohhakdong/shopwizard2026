package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdReviewMapper;
import com.shopwizard.product.model.ProdReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdReviewService {
    private final ProdReviewMapper prodReviewMapper;

    public List<ProdReview> selectList(Map<String, Object> params) { return prodReviewMapper.selectList(params); }
    public ProdReview select(Integer reviewNo) { return prodReviewMapper.select(reviewNo); }
    public void insert(ProdReview prodReview) { prodReviewMapper.insert(prodReview); }
    public void update(ProdReview prodReview) { prodReviewMapper.update(prodReview); }
    public void delete(Integer reviewNo) { prodReviewMapper.delete(reviewNo); }
    public List<ProdReview> selectListBest(Map<String, Object> params) { return prodReviewMapper.selectListBest(params); }
    public int selectListCount(Map<String, Object> params) { return prodReviewMapper.selectListCount(params); }
    public List<ProdReview> selectListPage(Map<String, Object> params) { return prodReviewMapper.selectListPage(params); }
    public List<ProdReview> selectListByProdCode(Map<String, Object> params) { return prodReviewMapper.selectListByProdCode(params); }
    public List<ProdReview> selectListByBrandName(Map<String, Object> params) { return prodReviewMapper.selectListByBrandName(params); }
}
