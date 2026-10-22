package com.shopwizard.message.mapper;

import com.shopwizard.message.model.SendRecevLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SendRecevLogMapper {
    List<SendRecevLog> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    SendRecevLog select(Map<String, Object> params);
    SendRecevLog selectTop(String docType);
}
