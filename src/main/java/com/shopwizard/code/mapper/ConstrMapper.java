package com.shopwizard.code.mapper;

import com.shopwizard.code.model.Constr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConstrMapper {
    List<Constr> findList(Map<String, Object> map);
    int insert(Constr constr);
    int update(Constr constr);
    int delete(@Param("constrCode") String constrCode);
    int deleteMultiple(@Param("list") List<Constr> list);
}
