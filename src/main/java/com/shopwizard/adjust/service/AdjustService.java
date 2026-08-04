package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustMapper;
import com.shopwizard.adjust.model.Adjust;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustService {

    private final AdjustMapper adjustMapper;

    public List<Adjust> selectList(Map<String, Object> params) { return adjustMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return adjustMapper.selectCount(params); }
    public List<Adjust> selectListBranchSale(Map<String, Object> params) { return adjustMapper.selectListBranchSale(params); }
    public int selectCountBranchSale(Map<String, Object> params) { return adjustMapper.selectCountBranchSale(params); }
    public List<Adjust> selectListShopBuy(Map<String, Object> params) { return adjustMapper.selectListShopBuy(params); }
    public int selectCountShopBuy(Map<String, Object> params) { return adjustMapper.selectCountShopBuy(params); }
    public Adjust select(Map<String, Object> params) { return adjustMapper.select(params); }
    public void insert(Adjust adjust) { adjustMapper.insert(adjust); }
    public void update(Adjust adjust) { adjustMapper.update(adjust); }
    public void delete(Adjust adjust) { adjustMapper.delete(adjust); }
}
