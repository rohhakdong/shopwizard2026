package com.shopwizard.order.mapper;

import com.shopwizard.external.shoplinker.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 샵링커 주문 원본 스테이징 테이블 {@code shopion.tOrdOrderShoplinker} 접근.
 * 수집한 XML 원본을 그대로 적재하고, 생성된 샵피온 주문번호로 역참조한다.
 */
@Mapper
public interface SlnkOrderMapper {
    /** 이미 수집된 주문코드인지 (중복 판단). */
    int countByCode(String code);

    /** XML 원본 1건 적재 (ShopionOrderNo = 생성된 주문번호). */
    void insert(Order order);
}
