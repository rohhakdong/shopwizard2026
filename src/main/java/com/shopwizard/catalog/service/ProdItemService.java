package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.ProdItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdItemService")
@RequiredArgsConstructor
public class ProdItemService {
    private final ProdItemMapper prodItemMapper;
    private final ProdMapper prodMapper;

    public List<ProdItem> selectList(Map<String, Object> params) { return prodItemMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodItemMapper.selectCount(params); }
    public ProdItem select(Map<String, Object> params) { return prodItemMapper.select(params); }
    public String selectItemCodeByAttrVal(Map<String, Object> params) { return prodItemMapper.selectItemCodeByAttrVal(params); }

    // 옵션을 바꾸면 게시된 상품(판매 상품이 있는 상품)은 판매 쪽 옵션도 같은 트랜잭션에서 맞춘다.
    // 판매 쪽 재고 수량(SupplyQty)은 주문 때 차감되므로 기존 옵션에서는 덮어쓰지 않는다.
    @Transactional
    public int insert(ProdItem prodItem) {
        int result = prodItemMapper.insert(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int update(ProdItem prodItem) {
        int result = prodItemMapper.update(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int updateItemlist(Map<String, Object> params) {
        int result = prodItemMapper.updateItemlist(params);
        syncIfPublished(params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        return result;
    }
    @Transactional
    public int delete(ProdItem prodItem) {
        int result = prodItemMapper.delete(prodItem);
        syncIfPublished(prodItem.getProdCode());
        return result;
    }
    @Transactional
    public int disable(String prodCode) {
        int result = prodItemMapper.disable(prodCode);
        syncIfPublished(prodCode);
        return result;
    }
    @Transactional
    public int enable(Map<String, Object> params) {
        int result = prodItemMapper.enable(params);
        syncIfPublished(params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode")));
        return result;
    }

    /**
     * 승인 게시 — 상품 본체(copy2shopion)가 먼저 복사되므로 이 시점엔 판매 상품이 이미 있다. 그래서 동기화(upsert)를 쓴다:
     * 처음 승인이면 옵션 전부가 새로 들어가고, 재승인·기존 상품이면 중복 키 오류 없이 내용만 갱신된다.
     * 판매 상품 자체가 없으면(본체 복사 없이 호출) 예전처럼 단순 복사한다.
     */
    @Transactional
    public int copy2Shopion(Map<String, Object> params) {
        String prodCode = params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode"));
        if (isPublished(prodCode)) return sync(prodCode);
        return prodItemMapper.copy2Shopion(params);
    }
    @Transactional public int delete2Shopion(String prodCode) { return prodItemMapper.delete2Shopion(prodCode); }

    private void syncIfPublished(String prodCode) {
        if (isPublished(prodCode)) sync(prodCode);
    }

    private int sync(String prodCode) {
        prodItemMapper.deleteMissing2Shopion(prodCode);
        return prodItemMapper.upsert2Shopion(prodCode);
    }

    private boolean isPublished(String prodCode) {
        return prodCode != null && !prodCode.isBlank() && prodMapper.countShopion(prodCode) > 0;
    }
}
