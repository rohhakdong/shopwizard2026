package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Event;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface EventMapper {
    List<Event> selectList(Map<String, Object> params);
    List<Event> selectListLevel(Map<String, Object> params);
    String selectEventCodeMax(Map<String, Object> params);
    Event select(Map<String, Object> params);
    String selectEventHtml(Map<String, Object> params);
    int insert(Event event);
    int update(Event event);
    int delete(Map<String, Object> params);
}
