package com.shopwizard.log.service;

import com.shopwizard.log.mapper.CustInfoMngrMapper;
import com.shopwizard.log.model.CustInfoMngr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustInfoMngrService {

    private final CustInfoMngrMapper custInfoMngrMapper;

    public List<CustInfoMngr> selectList(Map<String, Object> params) { return custInfoMngrMapper.selectList(params); }
    public void insert(CustInfoMngr custInfoMngr) { custInfoMngrMapper.insert(custInfoMngr); }
}
