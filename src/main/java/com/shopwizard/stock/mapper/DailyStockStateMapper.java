package com.shopwizard.stock.mapper;

import com.shopwizard.stock.model.DailyStockState;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface DailyStockStateMapper {
    List<DailyStockState> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<DailyStockState> selectListCheck(Map<String, Object> params);
    int selectCountCheck(Map<String, Object> params);
    List<DailyStockState> selectListShipDirect(Map<String, Object> params);
    DailyStockState select(Map<String, Object> params);
    int insert(DailyStockState dailyStockState);
    int insertEnterQty(DailyStockState dailyStockState);
    int insertShipQty(DailyStockState dailyStockState);
    int update(DailyStockState dailyStockState);
    int updateEnterQty(DailyStockState dailyStockState);
    int updateShipQty(DailyStockState dailyStockState);
    int updateEnterQtyAfterDiff(Map<String, Object> params);
    int delete(DailyStockState dailyStockState);
}
