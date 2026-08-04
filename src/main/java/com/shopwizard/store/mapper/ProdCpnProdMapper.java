package com.shopwizard.store.mapper;

import com.shopwizard.store.model.ProdCpnProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProdCpnProdMapper {
    List<ProdCpnProd> selectList(Map<String, Object> params);
    int insert(ProdCpnProd prodCpnProd);
    int delete(Map<String, Object> params);
}
