package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Cate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CateMapper {
    List<Cate> selectList(Map<String, Object> params);
    String selectCateCodeMax(Map<String, Object> params);
    Cate select(Map<String, Object> params);
    List<Cate> selectListLevel(Map<String, Object> params);
    List<Cate> selectListFinalTable(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    int selectCountChild(Map<String, Object> params);
    int insert(Cate cate);
    int update(Cate cate);
    int delete(Cate cate);
}
