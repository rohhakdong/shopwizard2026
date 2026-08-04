package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.FunctnMapper;
import com.shopwizard.authority.model.Functn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FunctnService {

    private final FunctnMapper functnMapper;

    public List<Functn> selectList(Map<String, Object> params) {
        return functnMapper.selectList(params);
    }

    public boolean selectAuth(Map<String, Object> params) {
        return functnMapper.selectAuth(params);
    }

    public Functn select(Map<String, Object> params) {
        return functnMapper.select(params);
    }

    @Transactional
    public int insert(Functn functn) {
        return functnMapper.insert(functn);
    }

    @Transactional
    public int update(Functn functn) {
        return functnMapper.update(functn);
    }

    @Transactional
    public int delete(Functn functn) {
        return functnMapper.delete(functn);
    }
}
