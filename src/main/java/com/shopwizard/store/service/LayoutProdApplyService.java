package com.shopwizard.store.service;

import com.shopwizard.store.mapper.LayoutProdApplyMapper;
import com.shopwizard.store.model.LayoutProdApply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LayoutProdApplyService {
    private final LayoutProdApplyMapper layoutProdApplyMapper;

    public List<LayoutProdApply> selectList(Map<String, Object> params) { return layoutProdApplyMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return layoutProdApplyMapper.selectCount(params); }
    public int insert(LayoutProdApply layoutProdApply) { return layoutProdApplyMapper.insert(layoutProdApply); }
    public int update(LayoutProdApply layoutProdApply) { return layoutProdApplyMapper.update(layoutProdApply); }
    public int delete(Map<String, Object> params) { return layoutProdApplyMapper.delete(params); }
}
