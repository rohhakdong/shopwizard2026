package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Functn;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface FunctnMapper {
    List<Functn> selectList(Map<String, Object> params);
    boolean selectAuth(Map<String, Object> params);
    Functn select(Map<String, Object> params);
    int insert(Functn functn);
    int update(Functn functn);
    int delete(Functn functn);
}
