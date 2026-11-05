package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("catalogProdItemMapper")
public interface ProdItemMapper {
    List<ProdItem> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdItem select(Map<String, Object> params);
    String selectItemCodeByAttrVal(Map<String, Object> params);
    int insert(ProdItem prodItem);
    int update(ProdItem prodItem);
    int updateItemlist(Map<String, Object> params);
    int delete(ProdItem prodItem);
    int deleteByProdCode(String prodCode);
    int disable(String prodCode);
    int enable(Map<String, Object> params);
    int copy2Shopion(Map<String, Object> params);
    /** 판매 쪽에만 남은 옵션 행을 지운다(등록 쪽에 없는 ItemCode). */
    int deleteMissing2Shopion(String prodCode);
    /** 등록 옵션을 판매 쪽에 upsert 한다 — 기존 옵션의 재고 수량 SupplyQty 는 유지, 새 옵션만 등록 쪽 재고로 시작. */
    int upsert2Shopion(String prodCode);
    int delete2Shopion(String prodCode);
}
