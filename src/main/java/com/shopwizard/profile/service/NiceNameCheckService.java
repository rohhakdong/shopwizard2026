package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.NiceNameCheckMapper;
import com.shopwizard.profile.model.NiceNameCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NiceNameCheckService {

    private final NiceNameCheckMapper niceNameCheckMapper;

    public List<NiceNameCheck> selectList(Map<String, Object> params) { return niceNameCheckMapper.selectList(params); }
    public NiceNameCheck select(String nameCheckUid) { return niceNameCheckMapper.select(nameCheckUid); }
    public int insert(NiceNameCheck niceNameCheck) { return niceNameCheckMapper.insert(niceNameCheck); }
}
