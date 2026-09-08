package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import com.shopwizard.catalog.model.ProdItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdService")
@RequiredArgsConstructor
public class ProdService {
    private final ProdMapper prodMapper;
    private final ProdItemMapper prodItemMapper;

    // 옵션이 없는(=단일 판매단위) 상품도 항상 이 ItemCode로 tCatProdItem에 "기본 아이템" 한 건이
    // 존재해야 한다는 게 이 프로젝트의 기존 데이터 전체(등록 시점/등록자에 무관하게 100%)에서
    // 관찰되는 확립된 규칙이다 — 그래야 상품↔옵션을 항상 조인으로 조회할 수 있다. 실제 다중
    // 옵션(사이즈/색상 등)은 이 번호 다음(20001, 20002, ...)부터 이어붙는다.
    private static final int DEFAULT_ITEM_CODE = 20000;
    private static final String DEFAULT_ITEM_ATTR_VAL = "선택사항없음";

    public List<Prod> selectList(Map<String, Object> params) { return prodMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodMapper.selectCount(params); }
    public Prod select(String prodCode) { return prodMapper.select(prodCode); }
    public String selectProdDesc(String prodCode) { return prodMapper.selectProdDesc(prodCode); }
    public String selectProdCode(String shopProdCode) { return prodMapper.selectProdCode(shopProdCode); }
    public List<ProdDashBoard> selectListDashboard(Map<String, Object> params) { return prodMapper.selectListDashboard(params); }

    @Transactional
    public int insert(Prod prod) {
        int result = prodMapper.insert(prod);

        ProdItem defaultItem = new ProdItem();
        defaultItem.setProdCode(prod.getProdCode());
        defaultItem.setItemCode(DEFAULT_ITEM_CODE);
        defaultItem.setAttrVal1(DEFAULT_ITEM_ATTR_VAL);
        defaultItem.setAttrVal2(""); defaultItem.setAttrVal3(""); defaultItem.setAttrVal4("");
        defaultItem.setSupplyQty(9999999);
        defaultItem.setLeadTime(prod.getLeadTime() != null ? prod.getLeadTime() : 3);
        defaultItem.setSaleYn(1);
        defaultItem.setSalePrice(0); defaultItem.setSupplyPrice(0); defaultItem.setBuyPrice(0); defaultItem.setOptionPrice(0);
        defaultItem.setState(0);
        defaultItem.setRegistId(prod.getRegistId()); defaultItem.setRegistName(prod.getRegistName());
        defaultItem.setChangeId(prod.getRegistId()); defaultItem.setChangeName(prod.getRegistName());
        prodItemMapper.insert(defaultItem);

        return result;
    }
    @Transactional public int update(Prod prod) { return prodMapper.update(prod); }
    @Transactional public int updatePrice(Map<String, Object> params) { return prodMapper.updatePrice(params); }
    @Transactional public int approv(Map<String, Object> params) { return prodMapper.approv(params); }
    @Transactional
    public int delete(Prod prod) {
        // insert()에서 항상 만들어주는 기본 아이템(ItemCode=20000)을 포함해, 이 상품에 딸린
        // tCatProdItem 옵션 행을 먼저 정리한다 — 안 지우면 상품마다 최소 1건씩 고아 데이터가 쌓인다.
        prodItemMapper.deleteByProdCode(prod.getProdCode());
        return prodMapper.delete(prod);
    }
    @Transactional public int copy2Shopion(Map<String, Object> params) { return prodMapper.copy2Shopion(params); }
    @Transactional public int delete2Shopion(String prodCode) { return prodMapper.delete2Shopion(prodCode); }
    @Transactional public int delete2ShopionStoreProd(String prodCode) { return prodMapper.delete2ShopionStoreProd(prodCode); }
    @Transactional public int delete2ShopionLayoutProd(String prodCode) { return prodMapper.delete2ShopionLayoutProd(prodCode); }
    @Transactional public int delete2ShopionEventProd(String prodCode) { return prodMapper.delete2ShopionEventProd(prodCode); }
}
