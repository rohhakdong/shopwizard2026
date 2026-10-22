package com.shopwizard.order.mapper;

import com.shopwizard.order.model.PricingItem;
import com.shopwizard.order.model.PricingProd;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 체크아웃 가격 계산용 읽기 쿼리(CheckoutPricingMapper.xml). */
@Mapper
public interface CheckoutPricingMapper {

    /** 상품 한 건. 없으면 null. */
    PricingProd selectProd(@Param("prodCode") String prodCode);

    /** 상품의 옵션 행 한 건. 없으면 null. */
    PricingItem selectItem(@Param("prodCode") String prodCode, @Param("itemCode") String itemCode);

    /** 기본행(선택사항없음, ItemCode=20000)을 뺀 "진짜 옵션" 개수. */
    int countRealOptions(@Param("prodCode") String prodCode);
}
