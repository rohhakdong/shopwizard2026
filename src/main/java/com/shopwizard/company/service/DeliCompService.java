package com.shopwizard.company.service;

import com.shopwizard.company.mapper.DeliCompMapper;
import com.shopwizard.company.model.DeliComp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliCompService {
    private final DeliCompMapper deliCompMapper;

    public List<DeliComp> getList(Map<String, Object> params) { return deliCompMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return deliCompMapper.selectCount(params); }
    public DeliComp get(Map<String, Object> params) { return deliCompMapper.select(params); }

    @Transactional
    public int insert(DeliComp deliComp) { return deliCompMapper.insert(deliComp); }
    @Transactional
    public int update(DeliComp deliComp) { return deliCompMapper.update(deliComp); }
    @Transactional
    public int delete(DeliComp deliComp) { return deliCompMapper.delete(deliComp); }
}
