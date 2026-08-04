package com.shopwizard.order.mapper;

import com.shopwizard.order.model.CurrOrderProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CurrOrderProdMapper {
    List<CurrOrderProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CurrOrderProd select(Map<String, Object> params);
    int insert(CurrOrderProd currOrderProd);
    int insertMatching(Map<String, Object> params);
    int update(CurrOrderProd currOrderProd);
    int delete(CurrOrderProd currOrderProd);
}
