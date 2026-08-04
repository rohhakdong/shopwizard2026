package com.shopwizard.order.mapper;

import com.shopwizard.order.model.ProdOptionChange;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProdOptionChangeMapper {
    List<ProdOptionChange> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdOptionChange select(Map<String, Object> params);
    int insert(ProdOptionChange prodOptionChange);
    int update(ProdOptionChange prodOptionChange);
    int delete(ProdOptionChange prodOptionChange);
}
