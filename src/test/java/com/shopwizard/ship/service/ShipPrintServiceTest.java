package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipPrintMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
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

/** 발주내역·발주내역2 조회 서비스 — 상점 필수, 기간 검증, 쪽 크기 상한, 엑셀 행 상한, 주소는 화면에 싣지 않음. */
class ShipPrintServiceTest {

    private ShipPrintMapper mapper;
    private ShipPrintService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ShipPrintMapper.class);
        service = new ShipPrintService(mapper);
    }

    private static Map<String, Object> raw() {
        Map<String, Object> r = new HashMap<>();
        r.put("orderNo", 1);
        r.put("orderProdNo", 1);
        r.put("prodQty", 2);
        r.put("orderProdQty", 1);
        r.put("buyPrice", 1000);
        r.put("recverAddr", "서울 어딘가");
        r.put("recverName", "홍길동");
        return r;
    }

    @Test
    void 상점이_없으면_400이고_조회하지_않는다() {
        assertThatThrownBy(() -> service.list1(" ", null, null, null, 0, 50)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("상점");
        assertThatThrownBy(() -> service.list2(null, null, null, null, null, null, null, null, 0, 50)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.excel1("", null, null, null, LocalDateTime.now())).isInstanceOf(ResponseStatusException.class);
        verify(mapper, never()).selectCount(any(), any(), any(), any());
    }

    @Test
    void 기간은_둘_다_비면_조건_없음_하나만_있거나_형식_순서가_틀리면_400() {
        assertThat(ShipPrintService.range("", null)).containsExactly(null, null);
        assertThat(ShipPrintService.range("2026-11-01", "2026-11-30")).containsExactly("2026-11-01", "2026-11-30");
        assertThatThrownBy(() -> ShipPrintService.range("2026-11-01", null)).hasMessageContaining("함께");
        assertThatThrownBy(() -> ShipPrintService.range("2026-11-30", "2026-11-01")).hasMessageContaining("늦습니다");
        assertThatThrownBy(() -> ShipPrintService.range("11/01", "11/30")).hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> ShipPrintService.range("2026-11-01'; DROP", "2026-11-30")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 날짜_기준은_S_O만_받고_기본은_S() {
        assertThat(ShipPrintService.dateType(null)).isEqualTo("S");
        assertThat(ShipPrintService.dateType("O")).isEqualTo("O");
        assertThatThrownBy(() -> ShipPrintService.dateType("X")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 목록은_쪽_크기를_200으로_제한하고_건수가_0이면_행을_조회하지_않는다() {
        when(mapper.selectCount("S1", null, null, null)).thenReturn(0);
        assertThat(service.list1("S1", null, null, null, 0, 9999).rows()).isEmpty();
        verify(mapper, never()).selectList(any(), any(), any(), any(), anyInt(), anyInt());

        when(mapper.selectCount("S1", "SHP001", "2026-11-01", "2026-11-02")).thenReturn(1);
        when(mapper.selectList("S1", "SHP001", "2026-11-01", "2026-11-02", 0, 200)).thenReturn(List.of(raw()));
        ShipPrintService.Page page = service.list1("S1", "SHP001", "2026-11-01", "2026-11-02", -5, 9999);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.rows().get(0).get("unitPrice")).isEqualTo(500L);
    }

    @Test
    void 발주내역2_화면_행에는_주소가_없고_엑셀_행에는_있다() {
        when(mapper.selectCount2("S1", null, "S", null, null, null, null, null)).thenReturn(1);
        when(mapper.selectList2("S1", null, "S", null, null, null, null, null, 0, 50)).thenReturn(List.of(raw()));
        when(mapper.selectList2("S1", null, "S", null, null, null, null, null, null, null)).thenReturn(List.of(raw()));
        when(mapper.selectShopName("S1")).thenReturn("테스트상점");

        ShipPrintService.Page page = service.list2("S1", null, null, null, null, null, null, null, null, null);
        assertThat(page.rows().get(0)).doesNotContainKey("recverAddr");

        ShipPrintService.Download file = service.excel2("S1", null, null, null, null, null, null, null);
        assertThat(file.fileName()).isEqualTo("발주내역2(테스트상점)_전체.xls");
        assertThat(file.bytes()).isNotEmpty();
    }

    @Test
    void 엑셀은_5만_행을_넘으면_기간을_좁히라고_400() {
        when(mapper.selectCount("S1", null, null, null)).thenReturn(ShipPrintService.MAX_EXCEL_ROWS + 1);

        assertThatThrownBy(() -> service.excel1("S1", null, null, null, LocalDateTime.now()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("기간을 좁혀");
        verify(mapper, never()).selectList(any(), any(), any(), any(), isNull(), isNull());
    }

    @Test
    void 발주내역_엑셀_파일명은_상점명과_시각이고_금지문자는_바뀐다() {
        when(mapper.selectCount(eq("S1"), any(), any(), any())).thenReturn(1);
        when(mapper.selectList(eq("S1"), any(), any(), any(), isNull(), isNull())).thenReturn(List.of(raw()));
        when(mapper.selectShopName("S1")).thenReturn("가/나:다");

        ShipPrintService.Download file = service.excel1("S1", null, null, null, LocalDateTime.of(2026, 11, 26, 9, 8, 7));

        assertThat(file.fileName()).isEqualTo("발주내역(가_나_다)20261126090807.xls");
    }
}
