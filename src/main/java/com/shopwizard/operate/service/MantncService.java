package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.MantncMapper;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.Mantnc;
import com.shopwizard.operate.model.MantncRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 유지보수 요청 관리(운영 > 유지보수, 레거시 wOprMantncList) — 요청 → 작업 예정일 → 작업완료 세 단계만.
 * 상태(ExecState): recipt 요청 · exec 작업중(예정일 입력됨) · cmplet 작업완료. 예정일 없이 바로 완료도 가능하다.
 * 작업완료된 요청은 이력이라 수정·삭제·예정일 변경이 안 되고(409), 완료 처리는 한 번만 된다(조건부 UPDATE 로 동시 처리 방지).
 *
 * <p>ownerId: 값이 있으면 그 사람이 올린 요청만 다룬다(범위 제한 계정). null 이면 전체(제한 없는 계정).
 * 예정일·완료 처리 권한(제한 없는 계정만)은 컨트롤러가 검사한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MantncService {

    public static final int DEFAULT_PAGE = 20;
    public static final int MAX_PAGE = 100;
    static final int TITLE_MAX = 50;
    static final int CONTENT_MAX = 10_000;
    static final int MENU_MAX = 255;
    static final int MEMO_MAX = 2_000;
    static final int COMPLETE_MAX = 255;

    private final MantncMapper mapper;

    public ListPage<Mantnc> list(String ownerId, String status, String keyword, String requester, String start, String end,
                                 Integer offset, Integer limit) {
        String st = status(status);
        String[] period = NoticeRules.period(start, end);
        int[] page = NoticeRules.paging(offset, limit, DEFAULT_PAGE, MAX_PAGE);
        String kw = NoticeRules.like(keyword);
        String rq = NoticeRules.like(requester);
        int total = mapper.selectCount(ownerId, st, kw, rq, period[0], period[1]);
        List<Mantnc> rows = total == 0 ? List.of() : mapper.selectList(ownerId, st, kw, rq, period[0], period[1], page[0], page[1]);
        return new ListPage<>(total, rows);
    }

    /** 상태별 건수(요청·작업중·작업완료)와 지연 건수. */
    public Map<String, Object> summary(String ownerId) {
        Map<String, Object> raw = mapper.selectSummary(ownerId);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : List.of("recipt", "exec", "cmplet", "overdue")) {
            Object v = raw == null ? null : raw.get(k);
            out.put(k, v instanceof Number n ? n.intValue() : 0);
        }
        return out;
    }

    /** 단건(내용 전문). 없으면 404, 남의 요청이면(ownerId 지정 시) 403. */
    public Mantnc get(Integer mantncNo, String ownerId) {
        Mantnc m = mantncNo == null ? null : mapper.select(mantncNo);
        if (m == null) throw new ResponseStatusException(NOT_FOUND, "요청을 찾을 수 없습니다.");
        if (ownerId != null && !ownerId.equals(m.getReciptId())) throw new ResponseStatusException(FORBIDDEN, "다른 사람의 요청은 다룰 수 없습니다.");
        return m;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mantnc create(MantncRequest req, String actorId, String actorName, String roleUid) {
        String title = title(req);
        String content = content(req);
        String menu = text(req.getMenuName(), MENU_MAX, "대상 화면");
        IdHolder key = new IdHolder();
        mapper.insert(title, content, menu, roleUid == null ? "" : roleUid, actorId, actorName, key);
        return mapper.select(key.getId());
    }

    /** 요청 내용 수정 — 작업완료 전만. */
    @Transactional(rollbackFor = Exception.class)
    public Mantnc update(MantncRequest req, String ownerId, String actorId, String actorName) {
        Mantnc current = get(req.getMantncNo(), ownerId);
        if (mapper.updateRequest(current.getMantncNo(), title(req), content(req), text(req.getMenuName(), MENU_MAX, "대상 화면"), actorId, actorName) == 0) {
            throw done();
        }
        return mapper.select(current.getMantncNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer mantncNo, String ownerId) {
        Mantnc current = get(mantncNo, ownerId);
        if (mapper.delete(current.getMantncNo()) == 0) throw done();
    }

    /** 작업 예정일 입력·변경(필수: yyyy-MM-dd) + 작업 메모(선택). 처음 입력하면 작업중이 된다. */
    @Transactional(rollbackFor = Exception.class)
    public Mantnc schedule(MantncRequest req, String actorId, String actorName) {
        Mantnc current = get(req.getMantncNo(), null);
        String date = date(req.getExecExpectDate());
        String memo = text(req.getExecCntnts(), MEMO_MAX, "작업 메모");
        if (mapper.updateSchedule(current.getMantncNo(), date, memo, actorId, actorName) == 0) throw done();
        return mapper.select(current.getMantncNo());
    }

    /** 작업완료 처리(완료 내용은 선택). 이미 완료면 409. */
    @Transactional(rollbackFor = Exception.class)
    public Mantnc complete(MantncRequest req, String actorId, String actorName) {
        Mantnc current = get(req.getMantncNo(), null);
        String cntnts = text(req.getCmpletCntnts(), COMPLETE_MAX, "완료 내용");
        if (mapper.updateComplete(current.getMantncNo(), cntnts, actorId, actorName) == 0) throw done();
        return mapper.select(current.getMantncNo());
    }

    // ── 입력 검증 ─────────────────────────────────────────────

    /** 상태 필터: 비면 전체, recipt·exec·cmplet·overdue. */
    static String status(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (s.equals("recipt") || s.equals("exec") || s.equals("cmplet") || s.equals("overdue")) return s;
        throw bad("상태가 올바르지 않습니다.");
    }

    static String title(MantncRequest r) {
        String t = r.getTitle() == null ? "" : r.getTitle().trim();
        if (t.isEmpty()) throw bad("제목을 입력하세요.");
        if (t.length() > TITLE_MAX) throw bad("제목은 " + TITLE_MAX + "자 이내여야 합니다.");
        return t;
    }

    static String content(MantncRequest r) {
        String c = r.getContent() == null ? "" : r.getContent().trim();
        if (c.isEmpty()) throw bad("요청 내용을 입력하세요.");
        if (c.length() > CONTENT_MAX) throw bad("요청 내용은 " + String.format("%,d", CONTENT_MAX) + "자 이내여야 합니다.");
        return c;
    }

    static String date(String raw) {
        if (raw == null || raw.isBlank()) throw bad("작업 예정일을 입력하세요.");
        try {
            return LocalDate.parse(raw.trim()).toString();
        } catch (DateTimeParseException e) {
            throw bad("작업 예정일은 yyyy-MM-dd 형식으로 입력하세요.");
        }
    }

    /** 선택 입력 글: 비면 null, 길면 400. */
    static String text(String raw, int max, String label) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw bad(label + "은(는) " + max + "자 이내여야 합니다.");
        return s;
    }

    private static ResponseStatusException done() {
        return new ResponseStatusException(CONFLICT, "이미 작업완료된 요청입니다.");
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
