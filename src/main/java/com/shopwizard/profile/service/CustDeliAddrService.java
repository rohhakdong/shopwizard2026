package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustDeliAddrMapper;
import com.shopwizard.profile.model.CustDeliAddr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustDeliAddrService {

    private final CustDeliAddrMapper custDeliAddrMapper;

    public List<CustDeliAddr> selectList(Map<String, Object> params) { return custDeliAddrMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custDeliAddrMapper.selectCount(params); }
    public CustDeliAddr select(Map<String, Object> params) { return custDeliAddrMapper.select(params); }
    public CustDeliAddr selectDefault(Integer custId) { return custDeliAddrMapper.selectDefault(custId); }
    public int insert(CustDeliAddr custDeliAddr) { return custDeliAddrMapper.insert(custDeliAddr); }
    public int update(CustDeliAddr custDeliAddr) { return custDeliAddrMapper.update(custDeliAddr); }
    public int updateDefaltYn(Integer custId) { return custDeliAddrMapper.updateDefaltYn(custId); }
    public int delete(Map<String, Object> params) { return custDeliAddrMapper.delete(params); }
    public int deleteByCustId(Integer custId) { return custDeliAddrMapper.deleteByCustId(custId); }
}
