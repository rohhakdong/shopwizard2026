package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderQuotDetail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderQuotDetailMapper {
    List<OrderQuotDetail> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderQuotDetail select(Map<String, Object> params);
    int selectMax(Map<String, Object> params);
    int insert(OrderQuotDetail orderQuotDetail);
    int update(OrderQuotDetail orderQuotDetail);
    int delete(OrderQuotDetail orderQuotDetail);
    int deleteByQuotNo(Map<String, Object> params);
}
