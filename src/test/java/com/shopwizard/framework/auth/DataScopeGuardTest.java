package com.shopwizard.framework.auth;

import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataScopeGuardTest {

    private DataScopeMapper mapper;
    private DataScopeGuard guard;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        guard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(guard, "mode", "enforce");
    }

    private static MockHttpServletRequest as(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        return req;
    }

    private static final Map<String, String> SHOP_ACCT = Map.of("loginId", "u", "shopCode", "MINE");
    private static final Map<String, String> UNRESTRICTED = Map.of("loginId", "admin", "shopCode", "", "chnlCode", "");

    @Test
    void 제한_없는_계정은_DB_조회_없이_통과() {
        MockHttpServletRequest req = as(UNRESTRICTED);
        assertDoesNotThrow(() -> guard.requireLine(req, 1, 1));
        assertDoesNotThrow(() -> guard.requireOrder(req, 1));
        assertDoesNotThrow(() -> guard.requireCatProd(req, "P"));
        assertDoesNotThrow(() -> guard.requirePrdProd(req, "P"));
        assertDoesNotThrow(() -> guard.requireAdjust(req, 1));
        verifyNoInteractions(mapper);
    }

    @Test
    void 내_라인은_통과_남의_라인은_403() {
        when(mapper.countLines(10, 1, "MINE", null)).thenReturn(1);
        when(mapper.countLines(11, 1, "MINE", null)).thenReturn(0);
        MockHttpServletRequest req = as(SHOP_ACCT);

        assertDoesNotThrow(() -> guard.requireLine(req, 10, 1));
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> guard.requireLine(req, 11, 1));
        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void 키가_비어_있으면_제한_계정은_403() {
        MockHttpServletRequest req = as(SHOP_ACCT);
        assertThrows(ResponseStatusException.class, () -> guard.requireLine(req, null, 1));
        assertThrows(ResponseStatusException.class, () -> guard.requireLine(req, 1, null));
        assertThrows(ResponseStatusException.class, () -> guard.requireCatProd(req, null));
    }

    @Test
    void 채널과_상점을_모두_쿼리에_전달한다_AND() {
        when(mapper.countLines(10, 1, "S", "C")).thenReturn(1);
        MockHttpServletRequest req = as(Map.of("loginId", "u", "shopCode", "S", "chnlCode", "C"));
        assertDoesNotThrow(() -> guard.requireLine(req, 10, 1));
        verify(mapper).countLines(10, 1, "S", "C");
    }

    @Test
    void 배치는_하나라도_남의_라인이면_전체_거부() {
        when(mapper.countLines(eq(1), anyInt(), eq("MINE"), any())).thenReturn(1);
        when(mapper.countLines(eq(2), anyInt(), eq("MINE"), any())).thenReturn(0);
        MockHttpServletRequest req = as(SHOP_ACCT);
        List<int[]> lines = List.of(new int[]{1, 1}, new int[]{1, 2}, new int[]{2, 1});

        assertThrows(ResponseStatusException.class,
                () -> guard.requireLines(req, lines, l -> l[0], l -> l[1]));
        assertDoesNotThrow(() -> guard.requireLines(req, lines.subList(0, 2), l -> l[0], l -> l[1]));
    }

    @Test
    void 상품은_상점_기준이고_채널_전용_계정은_제한하지_않는다() {
        when(mapper.countCatProd("OK", "MINE")).thenReturn(1);
        MockHttpServletRequest shop = as(SHOP_ACCT);
        assertDoesNotThrow(() -> guard.requireCatProd(shop, "OK"));
        assertThrows(ResponseStatusException.class, () -> guard.requireCatProd(shop, "NOT-MINE"));
        assertThrows(ResponseStatusException.class, () -> guard.requirePrdProd(shop, "NOT-MINE"));

        MockHttpServletRequest chnlOnly = as(Map.of("loginId", "u", "chnlCode", "C"));
        assertDoesNotThrow(() -> guard.requireCatProd(chnlOnly, "ANY"));
        verify(mapper, org.mockito.Mockito.never()).countCatProd(eq("ANY"), anyString());
    }

    @Test
    void 주문_헤더_정산_소유권() {
        when(mapper.countOrder(5, "MINE", null)).thenReturn(1);
        when(mapper.countAdjust("7", "MINE", null)).thenReturn(1);
        MockHttpServletRequest req = as(SHOP_ACCT);
        assertDoesNotThrow(() -> guard.requireOrder(req, 5));
        assertThrows(ResponseStatusException.class, () -> guard.requireOrder(req, 6));
        assertDoesNotThrow(() -> guard.requireAdjust(req, 7));
        assertThrows(ResponseStatusException.class, () -> guard.requireAdjust(req, 8));
    }

    @Test
    void 단건_행은_코드가_다르면_거부_행이_없으면_통과() {
        MockHttpServletRequest req = as(SHOP_ACCT);
        assertDoesNotThrow(() -> guard.requireRow(req, new Object(), "MINE", "ANY-CH"));
        assertThrows(ResponseStatusException.class, () -> guard.requireRow(req, new Object(), "OTHER", "ANY-CH"));
        assertDoesNotThrow(() -> guard.requireRow(req, null, null, null));

        MockHttpServletRequest chnl = as(Map.of("loginId", "u", "chnlCode", "C"));
        assertThrows(ResponseStatusException.class, () -> guard.requireRow(chnl, new Object(), "S", "OTHER-CH"));
        assertDoesNotThrow(() -> guard.requireRow(chnl, new Object(), "S", "C"));
    }

    @Test
    void 등록_수정의_상점코드는_세션_상점으로_고정() {
        assertEquals("MINE", guard.shopFor(as(SHOP_ACCT), "OTHER"));
        assertEquals("MINE", guard.shopFor(as(SHOP_ACCT), null));
        assertEquals("ANY", guard.shopFor(as(UNRESTRICTED), "ANY"));
    }

    @Test
    void off_모드는_검사하지_않는다() {
        ReflectionTestUtils.setField(guard, "mode", "off");
        assertDoesNotThrow(() -> guard.requireLine(as(SHOP_ACCT), 99, 1));
        assertEquals("OTHER", guard.shopFor(as(SHOP_ACCT), "OTHER"));
        verifyNoInteractions(mapper);
    }
}
