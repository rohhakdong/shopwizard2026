package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.ConsltInquiryMapper;
import com.shopwizard.operate.mapper.ConsltInquiryMapper.ConsltQuery;
import com.shopwizard.operate.model.ConsltInquiry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 고객문의 조회 조건 정리 — 처리여부·주문번호·연락처(숫자만)·LIKE 이스케이프·기간 검증, 건수 0 이면 행 조회 안 함, 쪽 크기 상한. */
class ConsltInquiryServiceTest {

    @Test
    void 조건은_정리되어_쿼리로_넘어간다() {
        ConsltQuery q = ConsltInquiryService.query(Map.ofEntries(
                Map.entry("pStartDate", "2026-11-01"), Map.entry("pEndDate", "2026-11-30"),
                Map.entry("pCustConsltType", "ship"), Map.entry("pAnswrYn", "N"), Map.entry("pOrderNo", " 2415368 "),
                Map.entry("pCustName", "홍%길동"), Map.entry("pPhone", "010-1234-5678"), Map.entry("pKeyword", "100%_환불"),
                Map.entry("pChnlCode", "CH1"), Map.entry("pSvcCode", "SHP001")));

        assertThat(q.getStart()).isEqualTo("2026-11-01");
        assertThat(q.getEnd()).isEqualTo("2026-11-30");
        assertThat(q.getType()).isEqualTo("ship");
        assertThat(q.getAnswered()).isZero();
        assertThat(q.getOrderNo()).isEqualTo("2415368");
        assertThat(q.getCustName()).isEqualTo("홍\\%길동");
        assertThat(q.getPhone()).isEqualTo("01012345678");
        assertThat(q.getKeyword()).isEqualTo("100\\%\\_환불");
        assertThat(q.getChnlCode()).isEqualTo("CH1");
        assertThat(q.getSvcCode()).isEqualTo("SHP001");
        assertThat(q.getReason()).isNull();
    }

    @Test
    void 빈_조건은_모두_조건_없음() {
        ConsltQuery q = ConsltInquiryService.query(Map.of("pType", "", "pAnswrYn", " "));
        assertThat(q.getStart()).isNull();
        assertThat(q.getAnswered()).isNull();
        assertThat(q.getOrderNo()).isNull();
        assertThat(q.getPhone()).isNull();
        assertThat(q.getKeyword()).isNull();
    }

    @Test
    void 처리여부_주문번호_연락처_기간의_잘못된_값은_400() {
        assertThat(ConsltInquiryService.answered("Y")).isEqualTo(1);
        assertThatThrownBy(() -> ConsltInquiryService.answered("maybe")).hasMessageContaining("처리");
        assertThatThrownBy(() -> ConsltInquiryService.orderNo("12a")).hasMessageContaining("숫자");
        assertThatThrownBy(() -> ConsltInquiryService.orderNo("12345678901")).hasMessageContaining("숫자");
        assertThatThrownBy(() -> ConsltInquiryService.phone("01")).hasMessageContaining("3자리");
        assertThat(ConsltInquiryService.phone("---")).isNull();
        assertThatThrownBy(() -> ConsltInquiryService.query(Map.of("pStartDate", "2026-11-30", "pEndDate", "2026-11-01"))).hasMessageContaining("늦습니다");
        assertThatThrownBy(() -> ConsltInquiryService.query(Map.of("pStartDate", "2026-11-30"))).hasMessageContaining("함께");
        assertThatThrownBy(() -> ConsltInquiryService.query(Map.of("pCustName", "가".repeat(101)))).hasMessageContaining("너무 깁니다");
    }

    @Test
    void 건수가_0이면_행을_조회하지_않고_쪽_크기는_100으로_제한한다() {
        ConsltInquiryMapper mapper = mock(ConsltInquiryMapper.class);
        ConsltInquiryService service = new ConsltInquiryService(mapper);
        when(mapper.selectCount(any())).thenReturn(0);
        assertThat(service.list(Map.of(), 0, 20).rows()).isEmpty();
        verify(mapper, never()).selectList(any(), anyInt(), anyInt());

        when(mapper.selectCount(any())).thenReturn(1);
        when(mapper.selectList(any(), anyInt(), anyInt())).thenReturn(List.of(new ConsltInquiry()));
        assertThat(service.list(Map.of(), -4, 9999).rows()).hasSize(1);
        ArgumentCaptor<Integer> lim = ArgumentCaptor.forClass(Integer.class);
        verify(mapper).selectList(any(), anyInt(), lim.capture());
        assertThat(lim.getValue()).isEqualTo(ConsltInquiryService.MAX_PAGE);
    }
}
