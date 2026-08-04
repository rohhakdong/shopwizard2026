package com.shopwizard.store.service;

import com.shopwizard.store.mapper.LayoutImgMapper;
import com.shopwizard.store.model.LayoutImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LayoutImgService {
    private final LayoutImgMapper layoutImgMapper;

    public List<LayoutImg> selectList(Map<String, Object> params) { return layoutImgMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return layoutImgMapper.selectCount(params); }
    public LayoutImg select(Map<String, Object> params) { return layoutImgMapper.select(params); }
    public int insert(LayoutImg layoutImg) { return layoutImgMapper.insert(layoutImg); }
    public int update(LayoutImg layoutImg) { return layoutImgMapper.update(layoutImg); }
    public int delete(Map<String, Object> params) { return layoutImgMapper.delete(params); }
}
