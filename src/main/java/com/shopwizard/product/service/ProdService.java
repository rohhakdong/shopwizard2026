package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdMapper;
import com.shopwizard.product.model.Prod;
import lombok.RequiredArgsConstructor;
import com.shopwizard.framework.util.DeleteResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdService {
    private static final int SOLD_OUT = -9;   // cSaleYnType: 완전품절

    private final ProdMapper prodMapper;

    public List<Prod> selectList(Map<String, Object> params) { return prodMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodMapper.selectCount(params); }
    public Prod select(Map<String, Object> params) { return prodMapper.select(params); }
    public Prod selectByShopProdCode(Map<String, Object> params) { return prodMapper.selectByShopProdCode(params); }
    public String selectShopProdCode(String shopProdCode) { return prodMapper.selectShopProdCode(shopProdCode); }
    public void insert(Prod prod) { prodMapper.insert(prod); }
    public void update(Prod prod) { prodMapper.update(prod); }
    public void updateReviewCntnts(Prod prod) { prodMapper.updateReviewCntnts(prod); }
    /**
     * 주문 이력이 있는 상품은 지우지 않고 완전품절(-9)로만 바꾼다 — 지우면 관리자 주문 조회에서 과거 주문이 사라진다.
     * 등록 쪽(tCatProd)의 판매여부도 같이 맞춘다(안 맞추면 다음 수정 때 등록 쪽 값이 판매 쪽을 되살린다).
     */
    public DeleteResult delete(String prodCode) {
        if (prodMapper.countOrderLines(prodCode) > 0) {
            Map<String, Object> p = new java.util.HashMap<>();
            p.put("prodCode", prodCode);
            p.put("saleYn", SOLD_OUT);
            prodMapper.updateSaleYn(p);
            prodMapper.updateCatSaleYn(p);
            return DeleteResult.ofSoldOut();
        }
        prodMapper.delete(prodCode);
        return DeleteResult.ofDeleted();
    }
    public List<Prod> selectListSearch(Map<String, Object> params) { return prodMapper.selectListSearch(params); }
    public int selectCountSearch(Map<String, Object> params) { return prodMapper.selectCountSearch(params); }
    public List<Prod> selectListKeyword(Map<String, Object> params) { return prodMapper.selectListKeyword(params); }
    public List<Prod> selectListBrand(Map<String, Object> params) { return prodMapper.selectListBrand(params); }
    public int selectSupplyQty(Map<String, Object> params) { return prodMapper.selectSupplyQty(params); }
    public int selectSupplyQtyItem(Map<String, Object> params) { return prodMapper.selectSupplyQtyItem(params); }
    public void updateSupplyQty(Map<String, Object> params) { prodMapper.updateSupplyQty(params); }
    /** 재고 절대값 설정 — 화면이 보던 값(expectedQty)이 그 사이 바뀌었으면 false. */
    public boolean setSupplyQty(Map<String, Object> params) { return prodMapper.setSupplyQty(params) == 1; }
    public void increaseSupplyQty(Map<String, Object> params) { prodMapper.increaseSupplyQty(params); }
    /** 재고가 충분할 때만 원자적으로 차감한다. 부족하면 false (재고는 그대로). */
    public boolean deductSupplyQtyIfEnough(Map<String, Object> params) { return prodMapper.deductSupplyQtyIfEnough(params) == 1; }
    public void updateSupplyQtyItem(Map<String, Object> params) { prodMapper.updateSupplyQtyItem(params); }
    public void updateCatSupplyQty(Map<String, Object> params) { prodMapper.updateCatSupplyQty(params); }
    public void updateCatSupplyQtyItem(Map<String, Object> params) { prodMapper.updateCatSupplyQtyItem(params); }
    public void updateSaleYn(Map<String, Object> params) { prodMapper.updateSaleYn(params); }
    public void updateSaleYnItem(Map<String, Object> params) { prodMapper.updateSaleYnItem(params); }
    public void updateCatSaleYn(Map<String, Object> params) { prodMapper.updateCatSaleYn(params); }
    public void updateCatSaleYnItem(Map<String, Object> params) { prodMapper.updateCatSaleYnItem(params); }
    public List<Map<String, Object>> selectListNaver(Map<String, Object> params) { return prodMapper.selectListNaver(params); }
}
