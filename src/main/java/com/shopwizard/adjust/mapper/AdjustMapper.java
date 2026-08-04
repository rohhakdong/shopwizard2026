package com.shopwizard.adjust.mapper;

import com.shopwizard.adjust.model.Adjust;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdjustMapper {
    List<Adjust> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<Adjust> selectListBranchSale(Map<String, Object> params);
    int selectCountBranchSale(Map<String, Object> params);
    List<Adjust> selectListShopBuy(Map<String, Object> params);
    int selectCountShopBuy(Map<String, Object> params);
    Adjust select(Map<String, Object> params);
    int insert(Adjust adjust);
    int update(Adjust adjust);
    int delete(Adjust adjust);
}
