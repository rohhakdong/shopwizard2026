package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CompStoreProdMapper;
import com.shopwizard.profile.model.CompStoreProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CompStoreProdService {

    private final CompStoreProdMapper compStoreProdMapper;

    public List<CompStoreProd> selectList(Map<String, Object> params) { return compStoreProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return compStoreProdMapper.selectCount(params); }
    public CompStoreProd select(Map<String, Object> params) { return compStoreProdMapper.select(params); }
    public int insert(CompStoreProd compStoreProd) { return compStoreProdMapper.insert(compStoreProd); }
    public int update(CompStoreProd compStoreProd) { return compStoreProdMapper.update(compStoreProd); }
    public int delete(Map<String, Object> params) { return compStoreProdMapper.delete(params); }
}
