package com.shopwizard.store.service;

import com.shopwizard.store.mapper.EventProdMapper;
import com.shopwizard.store.model.EventProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EventProdService {
    private final EventProdMapper eventProdMapper;

    public List<EventProd> selectList(Map<String, Object> params) { return eventProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return eventProdMapper.selectCount(params); }
    public List<EventProd> selectListAll(Map<String, Object> params) { return eventProdMapper.selectListAll(params); }
    public int insert(EventProd eventProd) { return eventProdMapper.insert(eventProd); }
    public int update(EventProd eventProd) { return eventProdMapper.update(eventProd); }
    public int delete(Map<String, Object> params) { return eventProdMapper.delete(params); }
}
