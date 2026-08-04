package com.shopwizard.store.mapper;

import com.shopwizard.store.model.LayoutProdApply;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface LayoutProdApplyMapper {
    List<LayoutProdApply> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    int insert(LayoutProdApply layoutProdApply);
    int update(LayoutProdApply layoutProdApply);
    int delete(Map<String, Object> params);
}
