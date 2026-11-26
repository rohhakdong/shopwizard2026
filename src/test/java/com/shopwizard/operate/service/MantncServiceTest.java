package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.MantncMapper;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.Mantnc;
import com.shopwizard.operate.model.MantncRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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

/** 유지보수 요청 서비스 — 입력 검증, 요청→작업중→완료 전이, 완료 후 거절(409), 남의 요청(403)·없는 요청(404), 목록 조건 정리. */
class MantncServiceTest {

    private MantncMapper mapper;
    private MantncService service;

    @BeforeEach
    void setUp() {
        mapper = mock(MantncMapper.class);
        service = new MantncService(mapper);
        when(mapper.insert(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            ((IdHolder) inv.getArgument(6)).setId(3);
            return 1;
        });
        Mantnc row = new Mantnc();
        row.setMantncNo(3);
        row.setReciptId("owner");
        row.setExecState("recipt");
        when(mapper.select(3)).thenReturn(row);
    }

    private static MantncRequest req() {
        MantncRequest r = new MantncRequest();
        r.setMantncNo(3);
        r.setTitle("  주문 화면 오류  ");
        r.setContent("  버튼이 안 눌립니다  ");
        return r;
    }

    @Test
    void 요청_등록은_제목과_내용을_다듬고_대상_화면은_비면_null_역할이_없으면_빈_문자열() {
        service.create(req(), "u", "사용자", null);

        verify(mapper).insert(eq("주문 화면 오류"), eq("버튼이 안 눌립니다"), isNull(), eq(""), eq("u"), eq("사용자"), any());
    }

    @Test
    void 제목_내용_대상화면_길이를_검증한다() {
        MantncRequest r = req();
        r.setTitle(" ");
        assertThatThrownBy(() -> service.create(r, "u", "n", "R")).hasMessageContaining("제목");
        r.setTitle("가".repeat(51));
        assertThatThrownBy(() -> service.create(r, "u", "n", "R")).hasMessageContaining("50자");
        r.setTitle("정상");
        r.setContent("  ");
        assertThatThrownBy(() -> service.create(r, "u", "n", "R")).hasMessageContaining("요청 내용");
        r.setContent("가".repeat(MantncService.CONTENT_MAX + 1));
        assertThatThrownBy(() -> service.create(r, "u", "n", "R")).hasMessageContaining("10,000자");
        r.setContent("내용");
        r.setMenuName("가".repeat(256));
        assertThatThrownBy(() -> service.create(r, "u", "n", "R")).hasMessageContaining("대상 화면");
        verify(mapper, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 작업_예정일은_필수이고_형식이_맞아야_하며_입력하면_저장한다() {
        MantncRequest r = req();
        assertThatThrownBy(() -> service.schedule(r, "h", "처리자")).hasMessageContaining("작업 예정일");
        r.setExecExpectDate("11/30");
        assertThatThrownBy(() -> service.schedule(r, "h", "처리자")).hasMessageContaining("yyyy-MM-dd");
        r.setExecExpectDate("2026-11-30");
        r.setExecCntnts("  ");
        when(mapper.updateSchedule(3, "2026-11-30", null, "h", "처리자")).thenReturn(1);
        service.schedule(r, "h", "처리자");
        verify(mapper).updateSchedule(3, "2026-11-30", null, "h", "처리자");
    }

    @Test
    void 완료_처리는_한_번만_된다() {
        MantncRequest r = req();
        r.setCmpletCntnts("재배포했습니다");
        when(mapper.updateComplete(3, "재배포했습니다", "h", "처리자")).thenReturn(1, 0);

        service.complete(r, "h", "처리자");
        assertThatThrownBy(() -> service.complete(r, "h", "처리자")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("이미 작업완료");

        r.setCmpletCntnts("가".repeat(256));
        assertThatThrownBy(() -> service.complete(r, "h", "처리자")).hasMessageContaining("완료 내용");
    }

    @Test
    void 완료된_요청은_수정_삭제_예정일_변경이_409() {
        MantncRequest r = req();
        r.setExecExpectDate("2026-12-01");
        when(mapper.updateRequest(anyInt(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.delete(3)).thenReturn(0);
        when(mapper.updateSchedule(anyInt(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.update(r, null, "u", "n")).hasMessageContaining("이미 작업완료");
        assertThatThrownBy(() -> service.delete(3, null)).hasMessageContaining("이미 작업완료");
        assertThatThrownBy(() -> service.schedule(r, "h", "n")).hasMessageContaining("이미 작업완료");
    }

    @Test
    void 남의_요청은_403_없는_요청은_404_제한_없는_계정은_누구_것이든_다룬다() {
        assertThatThrownBy(() -> service.get(3, "other")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("다른 사람");
        assertThatThrownBy(() -> service.update(req(), "other", "u", "n")).hasMessageContaining("다른 사람");
        assertThatThrownBy(() -> service.delete(3, "other")).hasMessageContaining("다른 사람");
        verify(mapper, never()).delete(anyInt());

        assertThat(service.get(3, "owner").getMantncNo()).isEqualTo(3);
        assertThat(service.get(3, null).getMantncNo()).isEqualTo(3);
        assertThatThrownBy(() -> service.get(99, null)).hasMessageContaining("찾을 수");
        assertThatThrownBy(() -> service.get(null, null)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 목록은_조건을_정리하고_상태를_검증하며_건수가_0이면_행을_조회하지_않는다() {
        when(mapper.selectCount(any(), any(), any(), any(), any(), any())).thenReturn(0);
        service.list("owner", "overdue", "50%_오류", "홍", "2026-11-01", "2026-11-30", -1, 9999);
        verify(mapper).selectCount("owner", "overdue", "50\\%\\_오류", "홍", "2026-11-01", "2026-11-30");
        verify(mapper, never()).selectList(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());

        when(mapper.selectCount(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(mapper.selectList(any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(new Mantnc()));
        assertThat(service.list(null, null, null, null, null, null, null, null).rows()).hasSize(1);
        verify(mapper).selectList(null, null, null, null, null, null, 0, MantncService.DEFAULT_PAGE);

        assertThatThrownBy(() -> service.list(null, "confrm", null, null, null, null, null, null)).hasMessageContaining("상태");
        assertThatThrownBy(() -> service.list(null, null, null, null, "2026-11-01", null, null, null)).hasMessageContaining("함께");
    }

    @Test
    void 요약은_없는_값을_0으로_채운다() {
        when(mapper.selectSummary(null)).thenReturn(Map.of("recipt", 2L, "exec", 1));
        Map<String, Object> s = service.summary(null);
        assertThat(s).containsEntry("recipt", 2).containsEntry("exec", 1).containsEntry("cmplet", 0).containsEntry("overdue", 0);
    }
}
