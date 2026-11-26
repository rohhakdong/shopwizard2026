package com.shopwizard.operate.web;

import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.company.service.SupplyCompNoticeService;
import com.shopwizard.company.web.SupplyCompNoticeController;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.Notice;
import com.shopwizard.operate.model.NoticeRequest;
import com.shopwizard.operate.service.ConsltInquiryService;
import com.shopwizard.operate.service.NoticeService;
import com.shopwizard.profile.service.CustInfoLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 운영 화면 컨트롤러의 데이터 범위 — 공지(채널·서비스·상점 제한)·공급사 공지(쓰기는 제한 없는 계정만)·고객문의(상점 불가, 조회 기록). */
class OperateControllersTest {

    private DataScopeMapper scopeMapper;
    private DataScopeGuard guard;
    private NoticeService noticeService;
    private SupplyCompNoticeService supplyService;
    private ConsltInquiryService consltService;
    private CustInfoLogService logService;
    private NoticeController notice;
    private SupplyCompNoticeController supply;
    private ConsltInquiryController conslt;

    @BeforeEach
    void setUp() {
        scopeMapper = mock(DataScopeMapper.class);
        guard = new DataScopeGuard(scopeMapper);
        ReflectionTestUtils.setField(guard, "mode", "enforce");
        noticeService = mock(NoticeService.class);
        supplyService = mock(SupplyCompNoticeService.class);
        consltService = mock(ConsltInquiryService.class);
        logService = mock(CustInfoLogService.class);
        notice = new NoticeController(noticeService, guard);
        supply = new SupplyCompNoticeController(supplyService, guard);
        conslt = new ConsltInquiryController(consltService, guard, logService);
        when(consltService.list(any(), any(), any())).thenReturn(new ListPage<>(0, List.of()));
    }

    private static MockHttpServletRequest login(Map<String, String> claims, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRemoteAddr("10.0.0.9");
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "shopCode", "S1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "chnlCode", "CH1");
    private static final Map<String, String> SVC = Map.of("loginId", "u", "name", "사용자", "svcCode", "SHP001");

    private static Notice row(String chnl) {
        Notice n = new Notice();
        n.setNoticeNo(7);
        n.setChnlCode(chnl);
        return n;
    }

    private static NoticeRequest body(String chnl) {
        NoticeRequest r = new NoticeRequest();
        r.setNoticeNo(7);
        r.setTitle("t");
        r.setStartDate("2026-11-26");
        r.setChnlCode(chnl);
        return r;
    }

    // ── 쇼핑몰 공지 ───────────────────────────────────────────

    @Test
    void 제한_없는_계정은_어느_채널_공지든_다루고_행위자는_세션에서_읽는다() {
        notice.create(body("ANY"), login(ALL, "/operate/notice"));

        verify(noticeService).create(any(), eq("u"), eq("사용자"));
    }

    @Test
    void 상점_제한_계정은_공지를_보지도_쓰지도_못한다() {
        MockHttpServletRequest shop = login(SHOP, "/operate/notice");
        assertThrows(ResponseStatusException.class, () -> notice.list(Map.of(), shop));
        assertThrows(ResponseStatusException.class, () -> notice.get(7, shop));
        assertThrows(ResponseStatusException.class, () -> notice.create(body("CH1"), shop));
        assertThrows(ResponseStatusException.class, () -> notice.update(body("CH1"), shop));
        assertThrows(ResponseStatusException.class, () -> notice.delete(7, shop));
        verify(noticeService, never()).create(any(), any(), any());
    }

    @Test
    void 채널_제한_계정은_자기_채널_공지만_고치고_등록할_때_채널이_자기_채널로_고정된다() {
        MockHttpServletRequest chnl = login(CHNL, "/operate/notice");
        when(noticeService.get(7)).thenReturn(row("CH1"));
        when(noticeService.get(8)).thenReturn(row("CH2"));

        NoticeRequest mine = body("CH2");   // 다른 채널로 보내도
        notice.create(mine, chnl);
        assertThat(mine.getChnlCode()).isEqualTo("CH1");

        notice.update(body("CH9"), chnl);   // 자기 채널 공지 수정은 가능, 채널은 고정
        notice.delete(7, chnl);
        NoticeRequest other = body("CH1");
        other.setNoticeNo(8);
        assertThrows(ResponseStatusException.class, () -> notice.update(other, chnl));   // 남의 채널 공지
        assertThrows(ResponseStatusException.class, () -> notice.delete(8, chnl));
        assertThrows(ResponseStatusException.class, () -> notice.get(8, chnl));
    }

    @Test
    void 서비스_제한_계정은_자기_서비스_채널의_공지만_다룬다() {
        MockHttpServletRequest svc = login(SVC, "/operate/notice");
        when(scopeMapper.countChnlInSvc("CH1", "SHP001")).thenReturn(1);
        when(scopeMapper.countChnlInSvc("OTHER", "SHP001")).thenReturn(0);
        when(noticeService.get(7)).thenReturn(row("CH1"));
        when(noticeService.get(8)).thenReturn(row("OTHER"));

        notice.create(body("CH1"), svc);
        assertThrows(ResponseStatusException.class, () -> notice.create(body("OTHER"), svc));
        NoticeRequest move = body("OTHER");   // 내 공지를 다른 서비스 채널로 옮길 수 없다
        assertThrows(ResponseStatusException.class, () -> notice.update(move, svc));
        NoticeRequest theirs = body("CH1");
        theirs.setNoticeNo(8);
        assertThrows(ResponseStatusException.class, () -> notice.update(theirs, svc));
        assertThrows(ResponseStatusException.class, () -> notice.delete(8, svc));
    }

    @Test
    void 페이지_값이_숫자가_아니면_400() {
        assertThrows(ResponseStatusException.class, () -> notice.list(Map.of("pPageSize", "x"), login(ALL, "/operate/notice/list")));
    }

    // ── 공급사 공지 ───────────────────────────────────────────

    @Test
    void 공급사_공지는_누구나_조회하지만_쓰기는_제한_없는_계정만() {
        when(supplyService.list(any(), any(), any(), any(), any(), any())).thenReturn(new ListPage<>(0, List.of()));
        when(supplyService.get(1)).thenReturn(new SupplyCompNotice());
        supply.list(Map.of());
        supply.get(1);

        supply.create(body(null), login(ALL, "/company/supply-comp-notice"));
        supply.update(body(null), login(ALL, "/company/supply-comp-notice"));
        supply.delete(1, login(ALL, "/company/supply-comp-notice"));
        verify(supplyService).create(any(), eq("u"), eq("사용자"));

        for (Map<String, String> limited : List.of(SHOP, CHNL, SVC)) {
            MockHttpServletRequest req = login(limited, "/company/supply-comp-notice");
            assertThrows(ResponseStatusException.class, () -> supply.create(body(null), req));
            assertThrows(ResponseStatusException.class, () -> supply.update(body(null), req));
            assertThrows(ResponseStatusException.class, () -> supply.delete(1, req));
        }
    }

    // ── 고객문의 ──────────────────────────────────────────────

    @Test
    void 고객문의_조회는_개인정보_조회_기록을_남긴다() {
        conslt.list(Map.of("pCustName", "홍길동", "pPhone", "010-1234"), login(ALL, "/operate/conslt/list"));

        ArgumentCaptor<String> exec = ArgumentCaptor.forClass(String.class);
        verify(logService).record(eq("/operate/conslt/list"), exec.capture(), eq("custConsltService.list(map)"), eq("consltInquiryMapper"),
                eq("tOprCustConslt"), eq("u"), eq("사용자"), eq("10.0.0.9"));
        assertThat(exec.getValue()).contains("pCustName=홍길동");
    }

    @Test
    void 고객문의는_상점_제한_계정이_볼_수_없고_기록도_조회도_하지_않는다() {
        assertThrows(ResponseStatusException.class, () -> conslt.list(Map.of(), login(SHOP, "/operate/conslt/list")));
        verify(consltService, never()).list(any(), any(), any());
        verify(logService, never()).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 고객문의_검증_실패는_기록하지_않고_기록이_실패해도_조회는_성공한다() {
        when(consltService.list(any(), any(), any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "x"));
        assertThrows(ResponseStatusException.class, () -> conslt.list(Map.of("pOrderNo", "abc"), login(ALL, "/operate/conslt/list")));
        verify(logService, never()).record(any(), any(), any(), any(), any(), any(), any(), any());

        org.mockito.Mockito.doReturn(new ListPage<>(0, List.of())).when(consltService).list(any(), any(), any());   // 위 스텁이 던지므로 when() 대신 doReturn
        doThrow(new IllegalStateException("log down")).when(logService).record(any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(conslt.list(Map.of(), login(ALL, "/operate/conslt/list")).total()).isZero();
    }
}
