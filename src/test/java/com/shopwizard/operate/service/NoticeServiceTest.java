package com.shopwizard.operate.service;

import com.shopwizard.company.mapper.SupplyCompNoticeMapper;
import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.company.service.SupplyCompNoticeService;
import com.shopwizard.operate.mapper.NoticeMapper;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.Notice;
import com.shopwizard.operate.model.NoticeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 공지(쇼핑몰·공급사) 입력 규칙과 서비스 — 제목·기간·팝업 검증, 채널·회사 실존, 목록 조건 정리. */
class NoticeServiceTest {

    private NoticeMapper mapper;
    private NoticeService service;

    @BeforeEach
    void setUp() {
        mapper = mock(NoticeMapper.class);
        service = new NoticeService(mapper);
        when(mapper.countChnl("CH1")).thenReturn(1);
        when(mapper.countComp("1234567890")).thenReturn(1);
        when(mapper.insert(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            ((IdHolder) inv.getArgument(5)).setId(11);
            return 1;
        });
        Notice saved = new Notice();
        saved.setNoticeNo(11);
        when(mapper.select(11)).thenReturn(saved);
    }

    private static NoticeRequest req() {
        NoticeRequest r = new NoticeRequest();
        r.setTitle("  배송 지연 안내  ");
        r.setContent("내용\n둘째 줄");
        r.setStartDate("2026-11-26");
        r.setChnlCode("CH1");
        return r;
    }

    @Test
    void 등록은_제목을_다듬고_종료일을_비우면_무기한_팝업이_아니면_기본_위치로_저장한다() {
        service.create(req(), "u", "사용자");

        ArgumentCaptor<NoticeRules.Clean> c = ArgumentCaptor.forClass(NoticeRules.Clean.class);
        verify(mapper).insert(c.capture(), eq("CH1"), isNull(), eq("u"), eq("사용자"), any());
        assertThat(c.getValue().getTitle()).isEqualTo("배송 지연 안내");
        assertThat(c.getValue().getEndDate()).isEqualTo("2999-12-31");
        assertThat(c.getValue().getPopupYn()).isZero();
        assertThat(c.getValue().getHoriznLoc()).isEqualTo(50);
        assertThat(c.getValue().getPopupWidth()).isEqualTo(386);
        assertThat(c.getValue().getPopupLenght()).isEqualTo(200);
    }

    @Test
    void 팝업이면_위치와_크기를_받고_범위를_검사한다() {
        NoticeRequest r = req();
        r.setPopupYn(1);
        r.setHoriznLoc(10);
        r.setVerticLoc(20);
        r.setPopupWidth(500);
        r.setPopupLenght(300);
        NoticeRules.Clean c = NoticeRules.clean(r, 50);
        assertThat(c.getPopupYn()).isEqualTo(1);
        assertThat(c.getHoriznLoc()).isEqualTo(10);
        assertThat(c.getPopupLenght()).isEqualTo(300);

        r.setPopupWidth(3001);
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("팝업 폭");
        r.setPopupWidth(-1);
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("팝업 폭");
        r.setPopupYn(2);
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("팝업 여부");
    }

    @Test
    void 제목_내용_기간_링크를_검증한다() {
        NoticeRequest r = req();
        r.setTitle(" ");
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("제목");
        r.setTitle("가".repeat(51));
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("50자");
        r.setTitle("정상");
        r.setContent("가".repeat(NoticeRules.CONTENT_MAX + 1));
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("내용");
        r.setContent("");
        r.setStartDate("2026-12-01");
        r.setEndDate("2026-11-30");
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("늦습니다");
        r.setStartDate("26/11/01");
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("yyyy-MM-dd");
        r.setStartDate("");
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("시작일");
        r.setStartDate("2026-11-01");
        r.setEndDate("2026-11-30");
        r.setLinkAddr("x".repeat(51));
        assertThatThrownBy(() -> NoticeRules.clean(r, 50)).hasMessageContaining("링크");
        assertThat(NoticeRules.clean(r, 255).getLinkAddr()).hasSize(51);   // 공급사 공지는 255자까지
    }

    @Test
    void 채널은_필수이고_실존해야_하며_회사코드는_선택이지만_입력하면_실존해야_한다() {
        NoticeRequest r = req();
        r.setChnlCode(" ");
        assertThatThrownBy(() -> service.create(r, "u", "n")).hasMessageContaining("채널을 선택");
        r.setChnlCode("NOPE");
        assertThatThrownBy(() -> service.create(r, "u", "n")).hasMessageContaining("채널을 찾을 수");
        r.setChnlCode("CH1");
        r.setCompCode("999");
        assertThatThrownBy(() -> service.create(r, "u", "n")).hasMessageContaining("회사코드");
        r.setCompCode("1234567890");
        service.create(r, "u", "n");
        verify(mapper).insert(any(), eq("CH1"), eq("1234567890"), any(), any(), any());
    }

    @Test
    void 없는_공지는_수정_삭제_조회가_404() {
        NoticeRequest r = req();
        r.setNoticeNo(999);
        assertThatThrownBy(() -> service.update(r, "u", "n")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("찾을 수");
        assertThatThrownBy(() -> service.delete(999)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.get(null)).isInstanceOf(ResponseStatusException.class);
        verify(mapper, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 수정은_기존_공지가_있을_때만_저장한다() {
        NoticeRequest r = req();
        r.setNoticeNo(11);
        service.update(r, "u2", "수정자");
        verify(mapper).update(eq(11), any(), eq("CH1"), isNull(), eq("u2"), eq("수정자"));
    }

    @Test
    void 목록은_와일드카드를_이스케이프하고_쪽_크기를_제한하며_상태_기간을_검증한다() {
        when(mapper.selectCount(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        service.list("CH1", null, "50%_할인", null, null, "ING", "2026-11-01", "2026-11-30", -3, 9999);
        verify(mapper).selectCount("CH1", null, "50\\%\\_할인", null, null, "ING", "2026-11-01", "2026-11-30");

        when(mapper.selectCount(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(mapper.selectList(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(new Notice()));
        assertThat(service.list(null, null, null, null, null, null, null, null, -3, 9999).rows()).hasSize(1);
        verify(mapper).selectList(null, null, null, null, null, null, null, null, 0, NoticeService.MAX_PAGE);

        assertThatThrownBy(() -> service.list(null, null, null, null, null, "X", null, null, 0, 20)).hasMessageContaining("게시 상태");
        assertThatThrownBy(() -> service.list(null, null, null, null, null, null, "2026-11-01", null, 0, 20)).hasMessageContaining("함께");
    }

    // ── 공급사 공지 ───────────────────────────────────────────

    @Test
    void 공급사_공지는_채널_없이_등록하고_링크는_255자까지다() {
        SupplyCompNoticeMapper sm = mock(SupplyCompNoticeMapper.class);
        when(sm.insert(any(), any(), any(), any())).thenAnswer(inv -> {
            ((IdHolder) inv.getArgument(3)).setId(5);
            return 1;
        });
        when(sm.select(5)).thenReturn(new SupplyCompNotice());
        SupplyCompNoticeService svc = new SupplyCompNoticeService(sm);

        NoticeRequest r = req();
        r.setChnlCode(null);
        r.setLinkAddr("x".repeat(200));
        svc.create(r, "u", "n");

        verify(sm).insert(any(), eq("u"), eq("n"), any());
        assertThatThrownBy(() -> svc.update(r, "u", "n")).isInstanceOf(ResponseStatusException.class);   // noticeNo 없음 → 404
        assertThatThrownBy(() -> svc.delete(7)).isInstanceOf(ResponseStatusException.class);
    }
}
