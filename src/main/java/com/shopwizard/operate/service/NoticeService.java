package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.NoticeMapper;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.Notice;
import com.shopwizard.operate.model.NoticeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 쇼핑몰 공지 관리(레거시 공지사항, shopion.tOprNotice) — 채널(·회사)별 공지의 목록·등록·수정·삭제.
 * 레거시와 달라진 점: 채널 필수를 서버가 검사하고(레거시는 채널 없이 조회하면 빈 목록), 정렬·쪽 나눔은 서버 고정값, 삭제는 레거시처럼 완전 삭제.
 * 데이터 범위(채널·서비스 제한)는 컨트롤러가 검사한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    public static final int DEFAULT_PAGE = 20;
    public static final int MAX_PAGE = 100;
    static final int LINK_MAX = 50;
    static final int COMP_MAX = 10;

    private final NoticeMapper mapper;

    public ListPage<Notice> list(String chnlCode, String svcCode, String title, String content, String registName, String status,
                                 String start, String end, Integer offset, Integer limit) {
        String st = NoticeRules.status(status);
        String[] period = NoticeRules.period(start, end);
        int[] page = NoticeRules.paging(offset, limit, DEFAULT_PAGE, MAX_PAGE);
        String chnl = blank(chnlCode);
        String svc = blank(svcCode);
        String t = NoticeRules.like(title);
        String c = NoticeRules.like(content);
        String r = NoticeRules.like(registName);
        int total = mapper.selectCount(chnl, svc, t, c, r, st, period[0], period[1]);
        List<Notice> rows = total == 0 ? List.of() : mapper.selectList(chnl, svc, t, c, r, st, period[0], period[1], page[0], page[1]);
        return new ListPage<>(total, rows);
    }

    /** 단건(내용 포함). 없으면 404. */
    public Notice get(Integer noticeNo) {
        Notice n = noticeNo == null ? null : mapper.select(noticeNo);
        if (n == null) throw new ResponseStatusException(NOT_FOUND, "공지를 찾을 수 없습니다.");
        return n;
    }

    @Transactional(rollbackFor = Exception.class)
    public Notice create(NoticeRequest req, String registId, String registName) {
        NoticeRules.Clean n = NoticeRules.clean(req, LINK_MAX);
        String chnl = requireChnl(req.getChnlCode());
        String comp = comp(req.getCompCode());
        IdHolder key = new IdHolder();
        mapper.insert(n, chnl, comp, registId, registName, key);
        return mapper.select(key.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Notice update(NoticeRequest req, String changeId, String changeName) {
        if (req.getNoticeNo() == null || mapper.select(req.getNoticeNo()) == null) throw new ResponseStatusException(NOT_FOUND, "공지를 찾을 수 없습니다.");
        NoticeRules.Clean n = NoticeRules.clean(req, LINK_MAX);
        String chnl = requireChnl(req.getChnlCode());
        String comp = comp(req.getCompCode());
        mapper.update(req.getNoticeNo(), n, chnl, comp, changeId, changeName);
        return mapper.select(req.getNoticeNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer noticeNo) {
        if (noticeNo == null || mapper.delete(noticeNo) == 0) throw new ResponseStatusException(NOT_FOUND, "공지를 찾을 수 없습니다.");
    }

    private String requireChnl(String chnlCode) {
        String c = blank(chnlCode);
        if (c == null) throw new ResponseStatusException(BAD_REQUEST, "채널을 선택하세요.");
        if (mapper.countChnl(c) == 0) throw new ResponseStatusException(BAD_REQUEST, "채널을 찾을 수 없습니다.");
        return c;
    }

    /** 회사코드는 선택 — 비우면 전체 고객 대상. 입력하면 실존하는 회사여야 한다. */
    private String comp(String compCode) {
        String c = blank(compCode);
        if (c == null) return null;
        if (c.length() > COMP_MAX || mapper.countComp(c) == 0) throw new ResponseStatusException(BAD_REQUEST, "회사코드를 찾을 수 없습니다.");
        return c;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
