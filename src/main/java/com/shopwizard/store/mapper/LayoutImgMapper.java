package com.shopwizard.store.mapper;

import com.shopwizard.store.model.LayoutImg;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface LayoutImgMapper {
    List<LayoutImg> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    LayoutImg select(Map<String, Object> params);
    int insert(LayoutImg layoutImg);
    int update(LayoutImg layoutImg);
    int delete(Map<String, Object> params);
}
