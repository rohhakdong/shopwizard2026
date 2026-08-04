package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Cust;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustMapper {
    List<Cust> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<Cust> selectListEmp(Map<String, Object> params);
    Cust select(Integer custId);
    Cust selectByLoginId(Map<String, Object> params);
    Cust selectForIdpw(Map<String, Object> params);
    int insert(Cust cust);
    int update(Cust cust);
    int updatePasswd(Cust cust);
    int updateLastLoginTime(Cust cust);
    int updateInfoOrder(Map<String, Object> params);
    int updateInfoOrderExcel(Map<String, Object> params);
    int updateInfoOrderShoplinker(Map<String, Object> params);
    int updateInfoShipDirect(Map<String, Object> params);
    int updateInfoShipDirectPrint(Map<String, Object> params);
    int updateInfoReturnDirect(Map<String, Object> params);
    int updateInfoOrderAll(Map<String, Object> params);
    int updateInfoOrderExcelAll(Map<String, Object> params);
    int updateInfoOrderShoplinkerAll(Map<String, Object> params);
    int updateInfoShipDirectAll(Map<String, Object> params);
    int updateInfoShipDirectPrintAll(Map<String, Object> params);
    int updateInfoReturnDirectAll(Map<String, Object> params);
    int delete(Integer custId);
}
