package com.shopwizard.company.service;

import com.shopwizard.company.mapper.CompMapper;
import com.shopwizard.company.model.Comp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service("companyCompService")
@RequiredArgsConstructor
public class CompService {
    private final CompMapper compMapper;

    public List<Comp> getList(Map<String, Object> params) { return compMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return compMapper.selectCount(params); }
    public Comp get(Map<String, Object> params) { return compMapper.select(params); }

    @Transactional
    public int insert(Comp comp) { return compMapper.insert(comp); }
    @Transactional
    public int update(Comp comp) { return compMapper.update(comp); }
    @Transactional
    public int delete(Map<String, Object> params) { return compMapper.delete(params); }
}
