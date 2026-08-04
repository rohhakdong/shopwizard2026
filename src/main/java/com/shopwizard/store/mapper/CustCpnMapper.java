package com.shopwizard.store.mapper;

import com.shopwizard.store.model.CustCpn;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustCpnMapper {
    List<CustCpn> selectList(Map<String, Object> params);
    CustCpn select(Integer cpnId);
    int insert(CustCpn custCpn);
    int update(CustCpn custCpn);
    int delete(Integer cpnId);
}
