package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.MantncMapper;
import com.shopwizard.operate.model.Mantnc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MantncService {

    private final MantncMapper mantncMapper;

    public List<Mantnc> selectList(Map<String, Object> params) { return mantncMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return mantncMapper.selectCount(params); }
    public Mantnc select(Map<String, Object> params) { return mantncMapper.select(params); }
    public int selectMax(Map<String, Object> params) { return mantncMapper.selectMax(params); }
}
