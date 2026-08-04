package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Menu;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface MenuMapper {
    List<Menu> selectList(Map<String, Object> params);
    String selectChildMax(Map<String, Object> params);
    List<Menu> selectListTable(Map<String, Object> params);
    int insert(Menu menu);
    int update(Menu menu);
    int delete(Map<String, Object> params);
}
