package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.Notice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {
    List<Notice> selectList(Map<String, Object> params);
    Notice select(Integer noticeNo);
    void insert(Notice notice);
    void update(Notice notice);
    void delete(Integer noticeNo);
    List<Notice> selectListPage(Map<String, Object> params);
    int selectCountPage(Map<String, Object> params);
    String selectCntnts(Integer noticeNo);
}
