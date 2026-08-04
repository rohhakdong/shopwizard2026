package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.MakerMapper;
import com.shopwizard.catalog.model.Maker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MakerService {
    private final MakerMapper makerMapper;

    public List<Maker> selectList(Map<String, Object> params) { return makerMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return makerMapper.selectCount(params); }
    public Maker select(Map<String, Object> params) { return makerMapper.select(params); }
    @Transactional public int insert(Maker maker) { return makerMapper.insert(maker); }
    @Transactional public int update(Maker maker) { return makerMapper.update(maker); }
    @Transactional public int delete(Map<String, Object> params) { return makerMapper.delete(params); }
}
