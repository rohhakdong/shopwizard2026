package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustDeliAddr;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustDeliAddrMapper {
    List<CustDeliAddr> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustDeliAddr select(Map<String, Object> params);
    CustDeliAddr selectDefault(Integer custId);
    int insert(CustDeliAddr custDeliAddr);
    int update(CustDeliAddr custDeliAddr);
    int updateDefaltYn(Integer custId);
    int delete(Map<String, Object> params);
    int deleteByCustId(Integer custId);
}
