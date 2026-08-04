package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DutyMapper;
import com.shopwizard.profile.model.Duty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DutyService {

    private final DutyMapper dutyMapper;

    public List<Duty> selectList(Map<String, Object> params) { return dutyMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return dutyMapper.selectCount(params); }
    public Duty select(Integer dutyId) { return dutyMapper.select(dutyId); }
    public int insert(Duty duty) { return dutyMapper.insert(duty); }
    public int update(Duty duty) { return dutyMapper.update(duty); }
    public int delete(Integer dutyId) { return dutyMapper.delete(dutyId); }
}
