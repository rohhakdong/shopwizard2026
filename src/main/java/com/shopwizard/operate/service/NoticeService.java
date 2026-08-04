package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.NoticeMapper;
import com.shopwizard.operate.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeMapper noticeMapper;

    public List<Notice> selectList(Map<String, Object> params) { return noticeMapper.selectList(params); }
    public Notice select(Integer noticeNo) { return noticeMapper.select(noticeNo); }
    public void insert(Notice notice) { noticeMapper.insert(notice); }
    public void update(Notice notice) { noticeMapper.update(notice); }
    public void delete(Integer noticeNo) { noticeMapper.delete(noticeNo); }
    public List<Notice> selectListPage(Map<String, Object> params) { return noticeMapper.selectListPage(params); }
    public int selectCountPage(Map<String, Object> params) { return noticeMapper.selectCountPage(params); }
    public String selectCntnts(Integer noticeNo) { return noticeMapper.selectCntnts(noticeNo); }
}
