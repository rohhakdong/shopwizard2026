package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdReview;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdReviewMapper {
    List<ProdReview> selectList(Map<String, Object> params);
    ProdReview select(Integer reviewNo);
    void insert(ProdReview prodReview);
    void update(ProdReview prodReview);
    void delete(Integer reviewNo);
    List<ProdReview> selectListBest(Map<String, Object> params);
    int selectListCount(Map<String, Object> params);
    List<ProdReview> selectListPage(Map<String, Object> params);
    List<ProdReview> selectListByProdCode(Map<String, Object> params);
    List<ProdReview> selectListByBrandName(Map<String, Object> params);
}
