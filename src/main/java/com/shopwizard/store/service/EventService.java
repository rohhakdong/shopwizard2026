package com.shopwizard.store.service;

import com.shopwizard.store.mapper.EventMapper;
import com.shopwizard.store.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {
    private final EventMapper eventMapper;

    public List<Event> selectList(Map<String, Object> params) { return eventMapper.selectList(params); }
    public List<Event> selectListLevel(Map<String, Object> params) { return eventMapper.selectListLevel(params); }
    public String selectEventCodeMax(Map<String, Object> params) { return eventMapper.selectEventCodeMax(params); }
    public Event select(Map<String, Object> params) { return eventMapper.select(params); }
    public String selectEventHtml(Map<String, Object> params) { return eventMapper.selectEventHtml(params); }
    public int insert(Event event) { return eventMapper.insert(event); }
    public int update(Event event) { return eventMapper.update(event); }
    public int delete(Map<String, Object> params) { return eventMapper.delete(params); }
}
