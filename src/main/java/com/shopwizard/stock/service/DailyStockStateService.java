package com.shopwizard.stock.service;

import com.shopwizard.stock.mapper.DailyStockStateMapper;
import com.shopwizard.stock.model.DailyStockState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyStockStateService {
    private final DailyStockStateMapper dailyStockStateMapper;
    public List<DailyStockState> selectList(Map<String, Object> params) { return dailyStockStateMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return dailyStockStateMapper.selectCount(params); }
    public List<DailyStockState> selectListCheck(Map<String, Object> params) { return dailyStockStateMapper.selectListCheck(params); }
    public int selectCountCheck(Map<String, Object> params) { return dailyStockStateMapper.selectCountCheck(params); }
    public List<DailyStockState> selectListShipDirect(Map<String, Object> params) { return dailyStockStateMapper.selectListShipDirect(params); }
    public DailyStockState select(Map<String, Object> params) { return dailyStockStateMapper.select(params); }
    public int insert(DailyStockState dailyStockState) { return dailyStockStateMapper.insert(dailyStockState); }
    public int insertEnterQty(DailyStockState dailyStockState) { return dailyStockStateMapper.insertEnterQty(dailyStockState); }
    public int insertShipQty(DailyStockState dailyStockState) { return dailyStockStateMapper.insertShipQty(dailyStockState); }
    public int update(DailyStockState dailyStockState) { return dailyStockStateMapper.update(dailyStockState); }
    public int updateEnterQty(DailyStockState dailyStockState) { return dailyStockStateMapper.updateEnterQty(dailyStockState); }
    public int updateShipQty(DailyStockState dailyStockState) { return dailyStockStateMapper.updateShipQty(dailyStockState); }
    public int updateEnterQtyAfterDiff(Map<String, Object> params) { return dailyStockStateMapper.updateEnterQtyAfterDiff(params); }
    public int delete(DailyStockState dailyStockState) { return dailyStockStateMapper.delete(dailyStockState); }
}
