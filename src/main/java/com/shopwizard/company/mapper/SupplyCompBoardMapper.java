package com.shopwizard.company.mapper;

import com.shopwizard.company.model.SupplyCompBoard;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyCompBoardMapper {
    List<SupplyCompBoard> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    SupplyCompBoard select(Map<String, Object> params);
}
