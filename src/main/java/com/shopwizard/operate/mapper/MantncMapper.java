package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.Mantnc;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface MantncMapper {
    List<Mantnc> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Mantnc select(Map<String, Object> params);
    int selectMax(Map<String, Object> params);
}
