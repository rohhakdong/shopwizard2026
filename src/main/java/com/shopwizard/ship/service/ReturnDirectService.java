package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ReturnDirectMapper;
import com.shopwizard.ship.model.ReturnDirect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnDirectService {
    private final ReturnDirectMapper returnDirectMapper;
    public List<ReturnDirect> selectList(Map<String, Object> params) { return returnDirectMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return returnDirectMapper.selectCount(params); }
    public ReturnDirect select(Map<String, Object> params) { return returnDirectMapper.select(params); }
    public int insert(ReturnDirect returnDirect) { return returnDirectMapper.insert(returnDirect); }
    public int insertReturnDirect(Map<String, Object> params) { return returnDirectMapper.insertReturnDirect(params); }
    public int update(Map<String, Object> params) { return returnDirectMapper.update(params); }
    public int updateAdjustSelectDate(Map<String, Object> params) { return returnDirectMapper.updateAdjustSelectDate(params); }
    public int updateAdjustSelectDateSchedule(Map<String, Object> params) { return returnDirectMapper.updateAdjustSelectDateSchedule(params); }
    public int delete(ReturnDirect returnDirect) { return returnDirectMapper.delete(returnDirect); }
    public int deleteRefundCancel(Map<String, Object> params) { return returnDirectMapper.deleteRefundCancel(params); }
}
