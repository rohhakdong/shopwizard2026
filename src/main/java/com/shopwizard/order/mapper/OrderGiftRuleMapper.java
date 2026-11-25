package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderGiftRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderGiftRuleMapper {
    List<OrderGiftRule> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderGiftRule select(Map<String, Object> params);
    int insert(OrderGiftRule orderGiftRule);
    int update(OrderGiftRule orderGiftRule);
    int delete(OrderGiftRule orderGiftRule);

    /** 이 상점이 있는가(등록·수정 검증). */
    int countShop(@Param("shopCode") String shopCode);
    /** 이 채널이 있는가. */
    int countChnl(@Param("chnlCode") String chnlCode);
    /** 이 발주단가 코드가 발주단가표에 있는가(사은품 코드 검증). */
    int countUprice(@Param("matchUpriceCode") String matchUpriceCode);
}
