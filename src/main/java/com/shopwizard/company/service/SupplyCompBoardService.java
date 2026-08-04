package com.shopwizard.company.service;

import com.shopwizard.company.mapper.SupplyCompBoardMapper;
import com.shopwizard.company.model.SupplyCompBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplyCompBoardService {
    private final SupplyCompBoardMapper supplyCompBoardMapper;

    public List<SupplyCompBoard> getList(Map<String, Object> params) { return supplyCompBoardMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return supplyCompBoardMapper.selectCount(params); }
    public SupplyCompBoard get(Map<String, Object> params) { return supplyCompBoardMapper.select(params); }

    @Transactional
    public int insert(SupplyCompBoard supplyCompBoard) { return supplyCompBoardMapper.insert(supplyCompBoard); }
    @Transactional
    public int update(SupplyCompBoard supplyCompBoard) { return supplyCompBoardMapper.update(supplyCompBoard); }
    @Transactional
    public int delete(SupplyCompBoard supplyCompBoard) { return supplyCompBoardMapper.delete(supplyCompBoard); }
}
