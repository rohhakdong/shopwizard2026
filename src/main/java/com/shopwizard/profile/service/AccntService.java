package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.AccntMapper;
import com.shopwizard.profile.model.Accnt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AccntService {

    private final AccntMapper accntMapper;

    public List<Accnt> selectList(Map<String, Object> params) { return accntMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return accntMapper.selectCount(params); }
    public Accnt select(Integer accntId) { return accntMapper.select(accntId); }
    public int insert(Accnt accnt) { return accntMapper.insert(accnt); }
    public int update(Accnt accnt) { return accntMapper.update(accnt); }
    public int delete(Integer accntId) { return accntMapper.delete(accntId); }
}
