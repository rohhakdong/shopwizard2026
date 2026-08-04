package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.AgreeMapper;
import com.shopwizard.operate.model.Agree;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AgreeService {
    private final AgreeMapper agreeMapper;

    public List<Agree> selectList(Map<String, Object> params) { return agreeMapper.selectList(params); }
    public Agree select(Map<String, Object> params) { return agreeMapper.select(params); }
    public void insert(Agree agree) { agreeMapper.insert(agree); }
    public void update(Agree agree) { agreeMapper.update(agree); }
    public void delete(Agree agree) { agreeMapper.delete(agree); }
    public String selectCntnts(Integer agreeNo) { return agreeMapper.selectCntnts(agreeNo); }
}
