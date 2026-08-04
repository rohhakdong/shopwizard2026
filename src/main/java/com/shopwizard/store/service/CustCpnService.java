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
    public int insert(CustCpn custCpn) { return custCpnMapper.insert(custCpn); }
    public int update(CustCpn custCpn) { return custCpnMapper.update(custCpn); }
    public int delete(Integer cpnId) { return custCpnMapper.delete(cpnId); }
}
