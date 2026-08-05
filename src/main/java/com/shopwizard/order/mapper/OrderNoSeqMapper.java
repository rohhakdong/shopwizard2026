package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderNoSeq;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderNoSeqMapper {
    void insert(OrderNoSeq orderNoSeq);
}
