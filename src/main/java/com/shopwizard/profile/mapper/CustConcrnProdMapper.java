package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustConcrnProd;
import com.shopwizard.profile.model.CustConcrnProdReport;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustConcrnProdMapper {
    List<CustConcrnProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<CustConcrnProdReport> selectListReport(Map<String, Object> params);
    int selectCountReport(Map<String, Object> params);
    CustConcrnProd select(Map<String, Object> params);
}
