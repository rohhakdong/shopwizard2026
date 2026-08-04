package com.shopwizard.store.service;

import com.shopwizard.store.mapper.DesignMapper;
import com.shopwizard.store.model.Design;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignService {
    private final DesignMapper designMapper;

    public List<Design> selectList(Map<String, Object> params) { return designMapper.selectList(params); }
    public int insert(Design design) { return designMapper.insert(design); }
    public int update(Design design) { return designMapper.update(design); }
    public int delete(Integer designId) { return designMapper.delete(designId); }
}
