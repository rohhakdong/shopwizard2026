package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.ConsltInquiryMapper;
import com.shopwizard.operate.mapper.ConsltInquiryMapper.ConsltQuery;
import com.shopwizard.operate.model.ConsltInquiry;
import com.shopwizard.operate.model.ListPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 고객문의 전체 조회(운영 > 고객문의, 레거시 wOprCustConsltList) — 주문상세에서 입력한 고객상담을 조건으로 찾아본다(읽기 전용).
 * 입력은 여기서 검증·정리하고(기간 형식, 주문번호 숫자, 연락처는 숫자만 3자리 이상, LIKE 와일드카드 이스케이프) 쿼리는 정해진 조건만 받는다.
 * 데이터 범위(상점 제한 불가·채널·서비스 제한)는 컨트롤러가 걸고, 채널·서비스 값은 {@code DataScopeFilter} 가 세션 값으로 고정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsltInquiryService {

    public static final int DEFAULT_PAGE = 20;
    public static final int MAX_PAGE = 100;
    private static final int CODE_MAX = 50;

    private final ConsltInquiryMapper mapper;

    /** 조회 조건 입력(요청 파라미터 그대로 — 검증은 {@link #query}). */
    public ListPage<ConsltInquiry> list(Map<String, String> p, Integer offset, Integer limit) {
        ConsltQuery q = query(p);
        int[] page = NoticeRules.paging(offset, limit, DEFAULT_PAGE, MAX_PAGE);
        int total = mapper.selectCount(q);
        List<ConsltInquiry> rows = total == 0 ? List.of() : mapper.selectList(q, page[0], page[1]);
        return new ListPage<>(total, rows);
    }

    static ConsltQuery query(Map<String, String> p) {
        String[] period = NoticeRules.period(p.get("pStartDate"), p.get("pEndDate"));
        return new ConsltQuery(period[0], period[1],
                code(p.get("pCustConsltType"), "상담구분"), code(p.get("pCustConsltReasonType"), "상담사유"), code(p.get("pConsltMediaType"), "상담매체"),
                answered(p.get("pAnswrYn")), code(p.get("pChnlCode"), "채널"), code(p.get("pSvcCode"), "서비스"),
                orderNo(p.get("pOrderNo")), NoticeRules.like(limit(p.get("pCustName"), "고객명")), phone(p.get("pPhone")),
                NoticeRules.like(limit(p.get("pKeyword"), "검색어")), NoticeRules.like(limit(p.get("pMngrName"), "상담자")));
    }

    /** 처리여부: Y 처리완료 → 1, N 미처리 → 0, 비면 전체. */
    static Integer answered(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (s.equals("Y")) return 1;
        if (s.equals("N")) return 0;
        throw bad("처리 여부가 올바르지 않습니다.");
    }

    static String orderNo(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (!s.matches("[0-9]{1,10}")) throw bad("주문번호는 숫자로 입력하세요.");
        return s;
    }

    /** 연락처: 하이픈·공백을 떼고 숫자 3자리 이상(너무 짧으면 거의 전체가 걸린다). */
    static String phone(String raw) {
        String s = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (s.isEmpty()) return null;
        if (s.length() < 3) throw bad("연락처는 숫자 3자리 이상 입력하세요.");
        if (s.length() > 20) throw bad("연락처가 너무 깁니다.");
        return s;
    }

    private static String code(String raw, String label) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        if (s.length() > CODE_MAX) throw bad(label + " 값이 올바르지 않습니다.");
        return s;
    }

    private static String limit(String raw, String label) {
        if (raw != null && raw.trim().length() > 100) throw bad(label + "이(가) 너무 깁니다.");
        return raw;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
