package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 대시보드 "배송 대기" 집계(배송 전 단계 라인 수). */
@ExtendWith(MockitoExtension.class)
class OrderProdServiceTest {

    @Mock private OrderProdMapper orderProdMapper;
    @InjectMocks private OrderProdService service;

    private static Map<String, Object> row(String state, Object cnt) {
        return Map.of("state", state, "cnt", cnt);
    }

    @Test
    void 상태별_건수를_세_상태로_정리하고_합계에_쓸_수_있다() {
        when(orderProdMapper.countByStates(any())).thenReturn(List.of(
                row("지불완료", 12L), row("출고지시", 3L), row("발주확인", 5L)));

        Map<String, Integer> counts = service.selectPendingShipCounts("SHP001");

        assertThat(counts).containsExactly(
                Map.entry("지불완료", 12), Map.entry("출고지시", 3), Map.entry("발주확인", 5));
    }

    @Test
    void 해당_라인이_없는_상태는_0으로_채운다() {
        when(orderProdMapper.countByStates(any())).thenReturn(List.of(row("출고지시", 4L)));

        assertThat(service.selectPendingShipCounts("SHP001"))
                .containsEntry("지불완료", 0).containsEntry("출고지시", 4).containsEntry("발주확인", 0);
    }

    @Test
    void 조회_대상_밖의_상태는_무시한다() {
        when(orderProdMapper.countByStates(any())).thenReturn(List.of(row("배송완료", 99L), row("지불완료", 1L)));

        Map<String, Integer> counts = service.selectPendingShipCounts("SHP001");

        assertThat(counts).hasSize(3).doesNotContainKey("배송완료");
        assertThat(counts.get("지불완료")).isEqualTo(1);
    }

    @Test
    void 서비스코드와_대상_상태를_쿼리에_넘긴다() {
        when(orderProdMapper.countByStates(any())).thenReturn(List.of());

        service.selectPendingShipCounts("SHP001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(orderProdMapper).countByStates(params.capture());
        assertThat(params.getValue()).containsEntry("svcCode", "SHP001")
                .containsEntry("states", List.of("지불완료", "출고지시", "발주확인"));
    }

    @Test
    void 서비스코드가_없으면_조회하지_않고_모두_0이다() {
        assertThat(service.selectPendingShipCounts(null).values()).containsOnly(0);
        assertThat(service.selectPendingShipCounts("  ").values()).containsOnly(0);

        verifyNoInteractions(orderProdMapper);
    }
}
