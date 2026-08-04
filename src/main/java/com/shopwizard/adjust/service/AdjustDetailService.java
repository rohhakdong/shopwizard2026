package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustDetailMapper;
import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.model.AdjustDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustDetailService {

    private final AdjustDetailMapper adjustDetailMapper;

    public List<AdjustDetail> selectList(Map<String, Object> params) { return adjustDetailMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return adjustDetailMapper.selectCount(params); }
    public List<Adjust> selectListSumBranch(Map<String, Object> params) { return adjustDetailMapper.selectListSumBranch(params); }
    public List<Adjust> selectListSumShop(Map<String, Object> params) { return adjustDetailMapper.selectListSumShop(params); }
    public AdjustDetail select(Map<String, Object> params) { return adjustDetailMapper.select(params); }
    public void insert(AdjustDetail adjustDetail) { adjustDetailMapper.insert(adjustDetail); }
    public void update(AdjustDetail adjustDetail) { adjustDetailMapper.update(adjustDetail); }
    public void delete(AdjustDetail adjustDetail) { adjustDetailMapper.delete(adjustDetail); }
    public void insertList(Map<String, Object> params) { adjustDetailMapper.insertList(params); }
    public void insertListByOrder(Map<String, Object> params) { adjustDetailMapper.insertListByOrder(params); }
    public void insertSchedule(Map<String, Object> params) { adjustDetailMapper.insertSchedule(params); }
    public void updateList(Map<String, Object> params) { adjustDetailMapper.updateList(params); }
    public void updateBranchSale(Map<String, Object> params) { adjustDetailMapper.updateBranchSale(params); }
    public void updateShopBuy(Map<String, Object> params) { adjustDetailMapper.updateShopBuy(params); }
}
