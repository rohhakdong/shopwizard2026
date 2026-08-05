package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderPayLogToss;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderPayLogTossMapper {
    void insert(OrderPayLogToss orderPayLogToss);
}
