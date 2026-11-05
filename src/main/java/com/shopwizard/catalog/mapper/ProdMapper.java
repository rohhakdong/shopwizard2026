package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("catalogProdMapper")
public interface ProdMapper {
    List<Prod> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Prod select(String prodCode);
    String selectProdDesc(String prodCode);
    String selectProdCode(String shopProdCode);
    List<ProdDashBoard> selectListDashboard(Map<String, Object> params);
    int insert(Prod prod);
    int update(Prod prod);
    int updatePrice(Map<String, Object> params);
    int approv(Map<String, Object> params);
    int delete(Prod prod);
    int copy2Shopion(Map<String, Object> params);
    /** 판매 상품(shopion.tPrdProd)에 같은 ProdCode 행이 있는지 — 있으면 "게시된 상품"이다(승인일 ApprovDate 는 레거시 데이터에서 비어 있어 못 쓴다). */
    int countShopion(String prodCode);
    /** 이 상품의 주문 라인(shopion.tOrdOrderProd) 수 — 있으면 판매 상품을 지우면 과거 주문이 관리자 조회에서 가려진다. */
    int countOrderLines(String prodCode);
    /** 등록 쪽 내용을 판매 상품에 덮어쓴다(재고 수량 SupplyQty 는 제외). */
    int sync2Shopion(String prodCode);
    int delete2Shopion(String prodCode);
    int delete2ShopionStoreProd(String prodCode);
    int delete2ShopionLayoutProd(String prodCode);
    int delete2ShopionEventProd(String prodCode);
}
