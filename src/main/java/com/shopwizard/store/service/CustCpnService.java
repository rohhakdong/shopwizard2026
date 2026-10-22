package com.shopwizard.store.service;

import com.shopwizard.store.mapper.CustCpnMapper;
import com.shopwizard.store.model.CustCpn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustCpnService {
    private final CustCpnMapper custCpnMapper;

    public List<CustCpn> selectList(Map<String, Object> params) { return custCpnMapper.selectList(params); }
    public CustCpn select(Integer cpnId) { return custCpnMapper.select(cpnId); }
}
