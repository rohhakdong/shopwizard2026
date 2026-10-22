package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.CustConslt;
import com.shopwizard.operate.model.CustConsltDashBoard;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustConsltMapper {
    List<CustConslt> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustConslt select(Integer custConsltNo);
    int countByCustId(Integer custId);
    List<CustConsltDashBoard> selectListDashboard(Map<String, Object> params);
}
