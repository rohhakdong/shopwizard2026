package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.CateMapper;
import com.shopwizard.catalog.model.Cate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CateService {
    private final CateMapper cateMapper;

    public List<Cate> selectList(Map<String, Object> params) { return cateMapper.selectList(params); }
    public String selectCateCodeMax(Map<String, Object> params) { return cateMapper.selectCateCodeMax(params); }
    public Cate select(Map<String, Object> params) { return cateMapper.select(params); }
    public List<Cate> selectListLevel(Map<String, Object> params) { return cateMapper.selectListLevel(params); }
    public List<Cate> selectListFinalTable(Map<String, Object> params) { return cateMapper.selectListFinalTable(params); }
    public int selectCount(Map<String, Object> params) { return cateMapper.selectCount(params); }
    public int selectCountChild(Map<String, Object> params) { return cateMapper.selectCountChild(params); }
    @Transactional public int insert(Cate cate) { return cateMapper.insert(cate); }
    @Transactional public int update(Cate cate) { return cateMapper.update(cate); }
    @Transactional public int delete(Cate cate) { return cateMapper.delete(cate); }
}
