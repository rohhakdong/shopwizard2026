package com.shopwizard.message.service;

import com.shopwizard.message.mapper.SendRecevLogMapper;
import com.shopwizard.message.model.SendRecevLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SendRecevLogService {

    private final SendRecevLogMapper sendRecevLogMapper;

    public List<SendRecevLog> selectList(Map<String, Object> params) { return sendRecevLogMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return sendRecevLogMapper.selectCount(params); }
    public SendRecevLog select(Map<String, Object> params) { return sendRecevLogMapper.select(params); }
    public SendRecevLog selectTop(String docType) { return sendRecevLogMapper.selectTop(docType); }
    public void insert(SendRecevLog sendRecevLog) { sendRecevLogMapper.insert(sendRecevLog); }
    public void update(SendRecevLog sendRecevLog) { sendRecevLogMapper.update(sendRecevLog); }
    public void delete(SendRecevLog sendRecevLog) { sendRecevLogMapper.delete(sendRecevLog); }
    public void updateTransResult(Integer logId) { sendRecevLogMapper.updateTransResult(logId); }
    public void updateTransError(Map<String, Object> params) { sendRecevLogMapper.updateTransError(params); }
}
