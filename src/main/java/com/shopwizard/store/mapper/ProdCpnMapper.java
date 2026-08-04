package com.shopwizard.store.mapper;

import com.shopwizard.store.model.ProdCpn;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProdCpnMapper {
    List<ProdCpn> selectList(Map<String, Object> params);
    ProdCpn select(Integer cpnId);
    int insert(ProdCpn prodCpn);
    int update(ProdCpn prodCpn);
    int delete(Integer cpnId);
}
