package com.shopwizard.store.service;

import com.shopwizard.store.mapper.LayoutMapper;
import com.shopwizard.store.model.Layout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LayoutService {
    private final LayoutMapper layoutMapper;

    public List<Layout> selectList(Map<String, Object> params) { return layoutMapper.selectList(params); }
    public String selectLayoutCodeMax(Map<String, Object> params) { return layoutMapper.selectLayoutCodeMax(params); }
    public Layout select(Map<String, Object> params) { return layoutMapper.select(params); }
    public String selectImgHtml(Map<String, Object> params) { return layoutMapper.selectImgHtml(params); }
    public int insert(Layout layout) { return layoutMapper.insert(layout); }
    public int update(Layout layout) { return layoutMapper.update(layout); }
    public int delete(Map<String, Object> params) { return layoutMapper.delete(params); }
}
