package com.shopwizard.company.service;

import com.shopwizard.company.mapper.SaleCompMapper;
import com.shopwizard.company.model.SaleComp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaleCompService {
    private final SaleCompMapper saleCompMapper;

    public List<SaleComp> getList(Map<String, Object> params) { return saleCompMapper.selectList(params); }
    public SaleComp get(Map<String, Object> params) { return saleCompMapper.select(params); }
    public String getMax(Map<String, Object> params) { return saleCompMapper.selectMax(params); }

    @Transactional
    public int insert(SaleComp saleComp) { return saleCompMapper.insert(saleComp); }
    @Transactional
    public int update(SaleComp saleComp) { return saleCompMapper.update(saleComp); }
    @Transactional
    public int delete(Map<String, Object> params) { return saleCompMapper.delete(params); }
}
