package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.OriginMapper;
import com.shopwizard.catalog.model.Origin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OriginService {
    private final OriginMapper originMapper;

    public List<Origin> selectList(Map<String, Object> params) { return originMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return originMapper.selectCount(params); }
    public Origin select(Map<String, Object> params) { return originMapper.select(params); }
    @Transactional public int insert(Origin origin) { return originMapper.insert(origin); }
    @Transactional public int update(Origin origin) { return originMapper.update(origin); }
    @Transactional public int delete(Map<String, Object> params) { return originMapper.delete(params); }
}
