package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdChnlMapper;
import com.shopwizard.catalog.mapper.ProdImgMapper;
import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import com.shopwizard.catalog.model.ProdItem;
import lombok.RequiredArgsConstructor;
import com.shopwizard.framework.util.DeleteResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdService")
@RequiredArgsConstructor
public class ProdService {
    private final ProdMapper prodMapper;
    private final ProdItemMapper prodItemMapper;
    private final ProdChnlMapper prodChnlMapper;
    private final ProdImgMapper prodImgMapper;

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
    /** 판매 상품(shopion.tPrdProd)에 같은 ProdCode 가 있으면 "게시된 상품"이다. 승인일(ApprovDate)은 레거시 데이터에서 비어 있어 기준이 못 된다. */
    public boolean isPublished(String prodCode) {
        return prodCode != null && !prodCode.isBlank() && prodMapper.countShopion(prodCode) > 0;
    }

    /** 수정 후 판매 상품에도 반영한다. 같은 트랜잭션이라 판매 쪽 반영이 실패하면 등록 쪽 수정도 롤백된다. */
    @Transactional
    public int update(Prod prod) {
        int result = prodMapper.update(prod);
        String prodCode = prod.getProdCode();
        if (prodCode == null || prodCode.isBlank()) prodCode = prodMapper.selectProdCode(prod.getShopProdCode());
        if (isPublished(prodCode)) prodMapper.sync2Shopion(prodCode);
        return result;
    }
    @Transactional
    public int updatePrice(Map<String, Object> params) {
        int result = prodMapper.updatePrice(params);
        Object prodCode = params.get("prodCode");
        if (prodCode != null && isPublished(String.valueOf(prodCode))) prodMapper.sync2Shopion(String.valueOf(prodCode));
        return result;
    }
    @Transactional public int approv(Map<String, Object> params) { return prodMapper.approv(params); }

    /**
     * 삭제 — 게시된 상품이면 판매 상품(+옵션·채널·이미지·상점/레이아웃/이벤트 진열)도 함께 지운다.
     * 주문 이력이 있는 상품은 지우지 않고 완전품절(-9)로만 바꾼다(게시된 상품이면 판매 쪽에도 같이 반영):
     * 관리자 주문 조회가 판매 상품과 조인하므로 지우면 과거 주문이 목록에서 사라진다.
     */
    @Transactional
    public DeleteResult delete(Prod prod) {
        String prodCode = prod.getProdCode();
        if (prodMapper.countOrderLines(prodCode) > 0) {
            prodMapper.markSoldOut(prodCode);
            if (isPublished(prodCode)) prodMapper.sync2Shopion(prodCode);
            return DeleteResult.ofSoldOut();
        }
        if (isPublished(prodCode)) {
            prodItemMapper.delete2Shopion(prodCode);
            prodChnlMapper.delete2Shopion(prodCode);
            prodImgMapper.delete2Shopion(prodCode);
            prodMapper.delete2ShopionStoreProd(prodCode);
            prodMapper.delete2ShopionLayoutProd(prodCode);
            prodMapper.delete2ShopionEventProd(prodCode);
            prodMapper.delete2Shopion(prodCode);
        }
        // insert()에서 항상 만들어주는 기본 아이템(ItemCode=20000)을 포함해, 이 상품에 딸린
        // tCatProdItem 옵션 행을 먼저 정리한다 — 안 지우면 상품마다 최소 1건씩 고아 데이터가 쌓인다.
        prodItemMapper.deleteByProdCode(prodCode);
        prodMapper.delete(prod);
        return DeleteResult.ofDeleted();
    }

    /** 승인 게시 — 판매 쪽에 아직 없으면 복사하고, 이미 있으면(재승인·기존 상품) 중복 오류 대신 내용을 동기화한다. */
    @Transactional
    public int copy2Shopion(Map<String, Object> params) {
        String prodCode = params.get("prodCode") == null ? null : String.valueOf(params.get("prodCode"));
        if (isPublished(prodCode)) return prodMapper.sync2Shopion(prodCode);
        return prodMapper.copy2Shopion(params);
    }
    @Transactional public int delete2Shopion(String prodCode) { return prodMapper.delete2Shopion(prodCode); }
    @Transactional public int delete2ShopionStoreProd(String prodCode) { return prodMapper.delete2ShopionStoreProd(prodCode); }
    @Transactional public int delete2ShopionLayoutProd(String prodCode) { return prodMapper.delete2ShopionLayoutProd(prodCode); }
    @Transactional public int delete2ShopionEventProd(String prodCode) { return prodMapper.delete2ShopionEventProd(prodCode); }
}
