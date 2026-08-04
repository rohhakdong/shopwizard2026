package com.shopwizard.store.mapper;

import com.shopwizard.store.model.EventBoard;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface EventBoardMapper {
    List<EventBoard> selectList(Map<String, Object> params);
    List<EventBoard> selectListPage(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    EventBoard select(Map<String, Object> params);
    int insert(EventBoard eventBoard);
    int update(EventBoard eventBoard);
    int delete(Map<String, Object> params);
    int deleteEvent(Map<String, Object> params);
}
