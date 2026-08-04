package com.shopwizard.order.mapper;

import com.shopwizard.order.model.ProdCodeMatch;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProdCodeMatchMapper {
    List<ProdCodeMatch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdCodeMatch select(Map<String, Object> params);
    int insert(ProdCodeMatch prodCodeMatch);
    int update(ProdCodeMatch prodCodeMatch);
    int delete(ProdCodeMatch prodCodeMatch);
}
