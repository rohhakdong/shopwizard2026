package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.RecentProdMapper;
import com.shopwizard.profile.model.RecentProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RecentProdService {

    private final RecentProdMapper recentProdMapper;

    public List<RecentProd> selectList(Map<String, Object> params) { return recentProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return recentProdMapper.selectCount(params); }
    public RecentProd select(Map<String, Object> params) { return recentProdMapper.select(params); }
    public int insert(RecentProd recentProd) { return recentProdMapper.insert(recentProd); }
    public int update(RecentProd recentProd) { return recentProdMapper.update(recentProd); }
    public int delete(Map<String, Object> params) { return recentProdMapper.delete(params); }
}
