package com.shopwizard.company.service;

import com.shopwizard.company.mapper.SupplyCompNoticeMapper;
import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.NoticeRequest;
import com.shopwizard.operate.service.NoticeRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 공급사 공지 관리(레거시 공급회사공지/상점공지, shopwizard.tCmpSupplyCompNotice) — 채널·회사 구분 없는 전역 공지의
 * 목록·등록·수정·삭제. 입력 규칙은 쇼핑몰 공지와 같고({@link NoticeRules}) 링크만 255자까지. 삭제는 레거시처럼 완전 삭제.
 * 쓰기 권한(제한 없는 계정만)은 컨트롤러가 검사한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplyCompNoticeService {

    static final int LINK_MAX = 255;
    static final int DEFAULT_PAGE = 20;
    static final int MAX_PAGE = 100;

    private final SupplyCompNoticeMapper mapper;

    public ListPage<SupplyCompNotice> list(String title, String status, String start, String end, Integer offset, Integer limit) {
        String st = NoticeRules.status(status);
        String[] period = NoticeRules.period(start, end);
        int[] page = NoticeRules.paging(offset, limit, DEFAULT_PAGE, MAX_PAGE);
        String t = NoticeRules.like(title);
        int total = mapper.selectCount(t, st, period[0], period[1]);
        List<SupplyCompNotice> rows = total == 0 ? List.of() : mapper.selectList(t, st, period[0], period[1], page[0], page[1]);
        return new ListPage<>(total, rows);
    }

    public SupplyCompNotice get(Integer noticeNo) {
        SupplyCompNotice n = noticeNo == null ? null : mapper.select(noticeNo);
        if (n == null) throw new ResponseStatusException(NOT_FOUND, "공지를 찾을 수 없습니다.");
        return n;
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplyCompNotice create(NoticeRequest req, String registId, String registName) {
        NoticeRules.Clean n = NoticeRules.clean(req, LINK_MAX);
        IdHolder key = new IdHolder();
        mapper.insert(n, registId, registName, key);
        return mapper.select(key.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplyCompNotice update(NoticeRequest req, String changeId, String changeName) {
        get(req.getNoticeNo());
        NoticeRules.Clean n = NoticeRules.clean(req, LINK_MAX);
        mapper.update(req.getNoticeNo(), n, changeId, changeName);
        return mapper.select(req.getNoticeNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer noticeNo) {
        if (noticeNo == null || mapper.delete(noticeNo) == 0) throw new ResponseStatusException(NOT_FOUND, "공지를 찾을 수 없습니다.");
    }
}
