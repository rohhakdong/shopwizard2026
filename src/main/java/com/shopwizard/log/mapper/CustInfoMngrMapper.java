package com.shopwizard.log.mapper;

import com.shopwizard.log.model.CustInfoMngr;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustInfoMngrMapper {
    List<CustInfoMngr> selectList(Map<String, Object> params);
    int insert(CustInfoMngr custInfoMngr);
}
