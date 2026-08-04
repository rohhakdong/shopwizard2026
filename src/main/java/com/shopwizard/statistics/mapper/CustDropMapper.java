package com.shopwizard.statistics.mapper;

import com.shopwizard.statistics.model.CustDrop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustDropMapper {
    int insert(CustDrop custDrop);
}
