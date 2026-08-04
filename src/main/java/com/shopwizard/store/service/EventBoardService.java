package com.shopwizard.store.service;

import com.shopwizard.store.mapper.EventBoardMapper;
import com.shopwizard.store.model.EventBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EventBoardService {
    private final EventBoardMapper eventBoardMapper;

    public List<EventBoard> selectList(Map<String, Object> params) { return eventBoardMapper.selectList(params); }
    public List<EventBoard> selectListPage(Map<String, Object> params) { return eventBoardMapper.selectListPage(params); }
    public int selectCount(Map<String, Object> params) { return eventBoardMapper.selectCount(params); }
    public EventBoard select(Map<String, Object> params) { return eventBoardMapper.select(params); }
    public int insert(EventBoard eventBoard) { return eventBoardMapper.insert(eventBoard); }
    public int update(EventBoard eventBoard) { return eventBoardMapper.update(eventBoard); }
    public int delete(Map<String, Object> params) { return eventBoardMapper.delete(params); }
    public int deleteEvent(Map<String, Object> params) { return eventBoardMapper.deleteEvent(params); }
}
