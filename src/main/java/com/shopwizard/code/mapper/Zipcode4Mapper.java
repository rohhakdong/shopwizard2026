package com.shopwizard.code.mapper;

import com.shopwizard.code.model.Zipcode4;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface Zipcode4Mapper {
    List<Zipcode4> findList(@Param("keyword") String keyword);
}
