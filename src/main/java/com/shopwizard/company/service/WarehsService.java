package com.shopwizard.company.service;

import com.shopwizard.company.mapper.WarehsMapper;
import com.shopwizard.company.model.Warehs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehsService {
    private final WarehsMapper warehsMapper;

    public List<Warehs> getList(Map<String, Object> params) { return warehsMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return warehsMapper.selectCount(params); }
    public Warehs get(Map<String, Object> params) { return warehsMapper.select(params); }

    @Transactional
    public int insert(Warehs warehs) { return warehsMapper.insert(warehs); }
    @Transactional
    public int update(Warehs warehs) { return warehsMapper.update(warehs); }
    @Transactional
    public int delete(Warehs warehs) { return warehsMapper.delete(warehs); }
}
