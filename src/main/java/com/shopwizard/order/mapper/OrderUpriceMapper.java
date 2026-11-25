package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderUprice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderUpriceMapper {
    List<OrderUprice> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderUprice select(Map<String, Object> params);
    int insert(OrderUprice orderUprice);
    int update(OrderUprice orderUprice);
    int delete(OrderUprice orderUprice);

    /** 같은 상점에서 같은 발주상품명칭이 같은 사용여부로 이미 있는 다른 코드 수(excludeCode 자신은 제외). */
    int countSameName(@Param("shopCode") String shopCode, @Param("orderProdName") String orderProdName,
                      @Param("useYn") int useYn, @Param("excludeCode") String excludeCode);

    /** 이 발주단가코드를 쓰는 단가매칭 규칙·발주견적 상세 수. */
    int countUsage(@Param("matchUpriceCode") String matchUpriceCode);
}
