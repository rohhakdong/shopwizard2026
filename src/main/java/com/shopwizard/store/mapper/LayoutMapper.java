package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Layout;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface LayoutMapper {
    List<Layout> selectList(Map<String, Object> params);
    String selectLayoutCodeMax(Map<String, Object> params);
    Layout select(Map<String, Object> params);
    String selectImgHtml(Map<String, Object> params);
    int insert(Layout layout);
    int update(Layout layout);
    int delete(Map<String, Object> params);
}
