package com.shopwizard.company.mapper;

import com.shopwizard.company.model.Comp;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("companyCompMapper")
public interface CompMapper {
    List<Comp> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Comp select(Map<String, Object> params);
    int insert(Comp comp);
    int update(Comp comp);
    int delete(Map<String, Object> params);
}
