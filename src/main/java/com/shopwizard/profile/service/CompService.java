package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CompMapper;
import com.shopwizard.profile.model.Comp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CompService {

    private final CompMapper compMapper;

    public List<Comp> selectList(Map<String, Object> params) { return compMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return compMapper.selectCount(params); }
    public Comp select(Map<String, Object> params) { return compMapper.select(params); }
    public int insert(Comp comp) { return compMapper.insert(comp); }
    public int update(Comp comp) { return compMapper.update(comp); }
    public int delete(String compCode) { return compMapper.delete(compCode); }
}
