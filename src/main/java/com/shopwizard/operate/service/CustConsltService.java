package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.CustConsltMapper;
import com.shopwizard.operate.model.CustConslt;
import com.shopwizard.operate.model.CustConsltDashBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustConsltService {
    private final CustConsltMapper custConsltMapper;

    public List<CustConslt> selectList(Map<String, Object> params) { return custConsltMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custConsltMapper.selectCount(params); }
    public CustConslt select(Integer custConsltNo) { return custConsltMapper.select(custConsltNo); }
    public void insert(CustConslt custConslt) { custConsltMapper.insert(custConslt); }
    public void update(CustConslt custConslt) { custConsltMapper.update(custConslt); }
    public void move(CustConslt custConslt) { custConsltMapper.move(custConslt); }
    public void delete(Integer custConsltNo) { custConsltMapper.delete(custConsltNo); }
    public int countByCustId(Integer custId) { return custConsltMapper.countByCustId(custId); }
    public List<CustConsltDashBoard> selectListDashboard(Map<String, Object> params) { return custConsltMapper.selectListDashboard(params); }
}
