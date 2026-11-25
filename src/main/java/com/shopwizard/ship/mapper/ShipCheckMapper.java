package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipCheckMapper {
    List<ShipCheck> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipCheck select(Map<String, Object> params);
    int insert(ShipCheck shipCheck);
    int updatePrintCount(ShipCheck shipCheck);
    /** 한 번의 발주확인(같은 ShipCheckDate)에 들어간 상점별 주문 수 — 발주 목록 헤더를 만드는 데 쓴다. */
    List<Map<String, Object>> selectBatchSummary(Map<String, Object> params);
    int delete(ShipCheck shipCheck);

    /** 업체(상점·창고) 화면용 — 허용된 상점들의 발주 목록. shopCode 가 있으면 그 상점만(허용 집합 안이어야 한다 — 호출자가 검사). */
    List<ShipCheck> selectListByShops(@Param("shopCodes") java.util.Collection<String> shopCodes, @Param("shopCode") String shopCode,
                                      @Param("startDate") String startDate, @Param("endDate") String endDate,
                                      @Param("offset") int offset, @Param("limit") int limit);
    int selectCountByShops(@Param("shopCodes") java.util.Collection<String> shopCodes, @Param("shopCode") String shopCode,
                           @Param("startDate") String startDate, @Param("endDate") String endDate);
    /** 창고에 연결된 사용 중인 상점(상점코드·상점명). */
    List<Map<String, Object>> selectShopsByWarehs(@Param("warehsCode") String warehsCode);
}
