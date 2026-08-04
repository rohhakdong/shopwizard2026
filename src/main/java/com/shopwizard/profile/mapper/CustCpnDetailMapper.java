package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustCpnDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustCpnDetailMapper {
    List<CustCpnDetail> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustCpnDetail select(Map<String, Object> params);
    int insert(CustCpnDetail custCpnDetail);
    int update(CustCpnDetail custCpnDetail);
    int updateByCustId(CustCpnDetail custCpnDetail);
    int delete(Map<String, Object> params);
    int disable(Map<String, Object> params);
    int enable(Map<String, Object> params);
}
