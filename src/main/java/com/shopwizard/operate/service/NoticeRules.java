package com.shopwizard.operate.service;

import com.shopwizard.operate.model.NoticeRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** 공지(쇼핑몰·공급사 공용) 입력 검증과 정규화. 통과한 값은 {@link Clean} 으로 돌려 준다(순수 함수). */
public final class NoticeRules {

    public static final int TITLE_MAX = 50;
    public static final int CONTENT_MAX = 20_000;
    public static final int POPUP_MAX = 3000;
    /** 종료일을 비웠을 때(레거시 기본값). */
    public static final String NO_END = "2999-12-31";

    private NoticeRules() {}

    /** 검증을 통과한 공지 값(MyBatis 가 getter 로 읽도록 클래스). 팝업이 아니면 위치·크기는 레거시 기본값(50·50·386·200)으로 저장한다. */
    @lombok.Value
    public static class Clean {
        String title;
        String content;
        String startDate;
        String endDate;
        int popupYn;
        int horiznLoc;
        int verticLoc;
        int popupWidth;
        int popupLenght;
        String linkAddr;
    }

    public static Clean clean(NoticeRequest r, int linkMax) {
        String title = r.getTitle() == null ? "" : r.getTitle().trim();
        if (title.isEmpty()) throw bad("제목을 입력하세요.");
        if (title.length() > TITLE_MAX) throw bad("제목은 " + TITLE_MAX + "자 이내여야 합니다.");
        String content = r.getContent() == null ? "" : r.getContent();
        if (content.length() > CONTENT_MAX) throw bad("내용은 " + String.format("%,d", CONTENT_MAX) + "자 이내여야 합니다.");

        LocalDate start = date(r.getStartDate(), "시작일");
        LocalDate end = r.getEndDate() == null || r.getEndDate().isBlank() ? LocalDate.parse(NO_END) : date(r.getEndDate(), "종료일");
        if (start.isAfter(end)) throw bad("시작일이 종료일보다 늦습니다.");

        int popup = r.getPopupYn() == null ? 0 : r.getPopupYn();
        if (popup != 0 && popup != 1) throw bad("팝업 여부는 0 또는 1 이어야 합니다.");
        int h = 50, v = 50, w = 386, l = 200;
        if (popup == 1) {
            h = range(r.getHoriznLoc(), 50, "가로 위치");
            v = range(r.getVerticLoc(), 50, "세로 위치");
            w = range(r.getPopupWidth(), 386, "팝업 폭");
            l = range(r.getPopupLenght(), 200, "팝업 길이");
        }
        String link = r.getLinkAddr() == null ? null : r.getLinkAddr().trim();
        if (link != null && link.isEmpty()) link = null;
        if (link != null && link.length() > linkMax) throw bad("링크 주소는 " + linkMax + "자 이내여야 합니다.");
        return new Clean(title, content, start.toString(), end.toString(), popup, h, v, w, l, link);
    }

    private static LocalDate date(String s, String label) {
        if (s == null || s.isBlank()) throw bad(label + "을(를) 입력하세요.");
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw bad(label + "은(는) yyyy-MM-dd 형식으로 입력하세요.");
        }
    }

    private static int range(Integer v, int dflt, String label) {
        int n = v == null ? dflt : v;
        if (n < 0 || n > POPUP_MAX) throw bad(label + "은(는) 0~" + POPUP_MAX + " 이어야 합니다.");
        return n;
    }

    /** 게시 상태 필터 값: 비면 전체, ING(게시중)·WAIT(예정)·END(종료). */
    public static String status(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (s.equals("ING") || s.equals("WAIT") || s.equals("END")) return s;
        throw bad("게시 상태가 올바르지 않습니다.");
    }

    /** 조회 기간: 둘 다 비면 {null,null}, 하나만 있거나 형식·순서가 틀리면 400. */
    public static String[] period(String start, String end) {
        boolean noS = start == null || start.isBlank();
        boolean noE = end == null || end.isBlank();
        if (noS && noE) return new String[]{null, null};
        if (noS || noE) throw bad("시작일과 종료일을 함께 입력하세요.");
        LocalDate s = date(start, "시작일");
        LocalDate e = date(end, "종료일");
        if (s.isAfter(e)) throw bad("시작일이 종료일보다 늦습니다.");
        return new String[]{s.toString(), e.toString()};
    }

    /** 쪽 나눔 값(범위 밖이면 기본값으로 정리). */
    public static int[] paging(Integer offset, Integer limit, int dflt, int max) {
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit < 1 ? dflt : Math.min(limit, max);
        return new int[]{off, lim};
    }

    /** LIKE 검색어의 와일드카드(% _ \)를 이스케이프한다 — 글자 그대로 찾도록. */
    public static String like(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
