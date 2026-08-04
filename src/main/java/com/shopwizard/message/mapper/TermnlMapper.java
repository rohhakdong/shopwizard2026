package com.shopwizard.message.mapper;

import com.shopwizard.message.model.Termnl;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface TermnlMapper {
    List<Termnl> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Termnl select(Map<String, Object> params);
    int insert(Termnl termnl);
    int update(Termnl termnl);
    int delete(Termnl termnl);
}
