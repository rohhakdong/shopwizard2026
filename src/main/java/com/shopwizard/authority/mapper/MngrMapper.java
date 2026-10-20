package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Mngr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface MngrMapper {
    List<Mngr> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Mngr select(Map<String, Object> params);
    List<Mngr> selectForLogin(Map<String, Object> params);
    Mngr selectInfo(String mngrUid);
    int selectProdApprovYn(String loginId);
    int insert(Mngr mngr);
    int update(Mngr mngr);
    int updatePasswd(@Param("mngrUid") String mngrUid, @Param("passwd") String passwd);
    int delete(String mngrUid);
}
