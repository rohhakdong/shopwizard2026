package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderKeywrd;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderKeywrdMapper {
    List<OrderKeywrd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderKeywrd select(Map<String, Object> params);
    /** 키워드로 대상 가져오기에 쓰는 사용 중(State=1) 키워드 전체 — 상점·구분·이름 순. */
    List<OrderKeywrd> selectAllActive();
    int insert(OrderKeywrd orderKeywrd);
    int update(OrderKeywrd orderKeywrd);
    int delete(OrderKeywrd orderKeywrd);

    int countShop(@Param("shopCode") String shopCode);
    /** 같은 (상점, 구분, 이름)이 이미 있는가(자기 자신 제외). 비교는 DB 동등 비교(대소문자·끝 공백 무시). */
    int countDuplicate(@Param("shopCode") String shopCode, @Param("keywrdSect") String keywrdSect,
                       @Param("keywrdName") String keywrdName, @Param("exceptSeq") Integer exceptSeq);
}
