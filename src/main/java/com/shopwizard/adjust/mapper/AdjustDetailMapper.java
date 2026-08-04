package com.shopwizard.adjust.mapper;

import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.model.AdjustDetail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdjustDetailMapper {
    List<AdjustDetail> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<Adjust> selectListSumBranch(Map<String, Object> params);
    List<Adjust> selectListSumShop(Map<String, Object> params);
    AdjustDetail select(Map<String, Object> params);
    int insert(AdjustDetail adjustDetail);
    int update(AdjustDetail adjustDetail);
    int delete(AdjustDetail adjustDetail);
    int insertList(Map<String, Object> params);
    int insertListByOrder(Map<String, Object> params);
    int insertSchedule(Map<String, Object> params);
    int updateList(Map<String, Object> params);
    int updateBranchSale(Map<String, Object> params);
    int updateShopBuy(Map<String, Object> params);
}
