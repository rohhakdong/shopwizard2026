package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.DeliRstrctZipcodeMapper;
import com.shopwizard.ship.mapper.ShipPlaceRuleMapper;
import com.shopwizard.ship.model.ShipPlaceRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 배송제한 규칙(저장 검증) · 배송제한 우편번호(붙여넣기 읽기·일괄 등록) · 배송 데이터 생성의 규칙 적용. */
class ShipPlaceRuleServiceTest {

    private final ShipPlaceRuleMapper mapper = mock(ShipPlaceRuleMapper.class);
    private final ShipPlaceRuleService service = new ShipPlaceRuleService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.countShop(anyString())).thenReturn(1);
        when(mapper.countChnl(anyString())).thenReturn(1);
        when(mapper.select(any())).thenReturn(null);
        doAnswer(inv -> { ((ShipPlaceRule) inv.getArgument(0)).setRuleNo(11); return 1; }).when(mapper).insert(any());
    }

    private static ShipPlaceRule in() {
        ShipPlaceRule r = new ShipPlaceRule();
        r.setShopCode(" 3018508386A1 ");
        r.setRuleName("택배비인상 ★");
        r.setZipSect("DeliFeeUpStar");
        r.setAddrPatterns("%서울%강남구%\n\n  %경기%광주%중대동%  \n%서울%강남구%");
        r.setShipPlace("택배비인상");
        r.setNamePrefix("★");
        r.setRegistId("u");
        return r;
    }

    private void expectBad(ShipPlaceRule r, String part) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(r));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(part), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 등록하면_패턴의_빈_줄과_중복을_정리하고_사용_중으로_시작한다() {
        service.create(in());

        ArgumentCaptor<ShipPlaceRule> cap = ArgumentCaptor.forClass(ShipPlaceRule.class);
        verify(mapper).insert(cap.capture());
        ShipPlaceRule row = cap.getValue();
        assertEquals("3018508386A1", row.getShopCode());
        assertEquals("%서울%강남구%\n%경기%광주%중대동%", row.getAddrPatterns());
        assertEquals("", row.getExceptAddrPatterns());
        assertEquals("", row.getProdName());
        assertEquals("", row.getExceptChnlCodes());
        assertEquals(1, row.getState());
    }

    @Test
    void 구분과_주소_패턴이_모두_비어도_저장된다_전체_대상_기본_시트_규칙() {
        ShipPlaceRule r = in(); r.setZipSect(null); r.setAddrPatterns(null);

        service.create(r);

        verify(mapper).insert(any());
    }

    @Test
    void 시트명은_필수_31자_이하이고_엑셀_금지_문자를_쓸_수_없다() {
        ShipPlaceRule r = in(); r.setShipPlace(" "); expectBad(r, "시트명");
        r = in(); r.setShipPlace("가".repeat(32)); expectBad(r, "시트명");
        for (String bad : new String[]{"a[b", "a]b", "a:b", "a*b", "a?b", "a/b", "a\\b"}) {
            ShipPlaceRule x = in(); x.setShipPlace(bad); expectBad(x, "시트명");
        }
        r = in(); r.setShipPlace("가".repeat(31));
        service.create(r);
        verify(mapper).insert(any());
    }

    @Test
    void 길이_줄수_구분_형식을_검사한다() {
        ShipPlaceRule r = in(); r.setShopCode(" "); expectBad(r, "상점");
        r = in(); r.setNamePrefix("가".repeat(11)); expectBad(r, "수취인 표시");
        r = in(); r.setProdName("가".repeat(51)); expectBad(r, "상품명");
        r = in(); r.setZipSect("구분 공백 있음"); expectBad(r, "우편번호 구분");
        r = in(); r.setAddrPatterns("가".repeat(101)); expectBad(r, "주소 패턴");
        r = in(); r.setAddrPatterns(String.join("\n", java.util.Collections.nCopies(1, "x")) + "\n" + java.util.stream.IntStream.range(0, 201).mapToObj(i -> "%p" + i + "%").collect(java.util.stream.Collectors.joining("\n")));
        expectBad(r, "최대 200줄");
        r = in(); r.setExceptAddrPatterns(java.util.stream.IntStream.range(0, 51).mapToObj(i -> "%e" + i + "%").collect(java.util.stream.Collectors.joining("\n")));
        expectBad(r, "최대 50줄");
        r = in(); r.setState(2); expectBad(r, "사용 여부");
        r = in(); r.setRemark("가".repeat(51)); expectBad(r, "비고");
    }

    @Test
    void 제외_채널은_쉼표_공백을_정리하고_실존하는_채널만_허용한다() {
        ShipPlaceRule r = in(); r.setExceptChnlCodes(" CH1, CH2  CH1 ,");

        service.create(r);

        ArgumentCaptor<ShipPlaceRule> cap = ArgumentCaptor.forClass(ShipPlaceRule.class);
        verify(mapper).insert(cap.capture());
        assertEquals("CH1,CH2", cap.getValue().getExceptChnlCodes());
        clearInvocations(mapper);

        when(mapper.countChnl("NOPE")).thenReturn(0);
        ShipPlaceRule bad = in(); bad.setExceptChnlCodes("CH1,NOPE");
        expectBad(bad, "없는 채널");
        ShipPlaceRule tooLong = in(); tooLong.setExceptChnlCodes("가".repeat(13));
        expectBad(tooLong, "채널 코드");

        when(mapper.countShop("NOSHOP")).thenReturn(0);
        ShipPlaceRule noShop = in(); noShop.setShopCode("NOSHOP");
        expectBad(noShop, "없는 상점");
    }

    @Test
    void 수정은_사용_여부를_비우면_지금_값을_유지하고_없는_규칙은_400이다() {
        ShipPlaceRule e = new ShipPlaceRule(); e.setRuleNo(3); e.setState(0);
        when(mapper.select(Map.of("ruleNo", 3))).thenReturn(e);
        ShipPlaceRule req = in(); req.setRuleNo(3);

        service.modify(req);

        ArgumentCaptor<ShipPlaceRule> cap = ArgumentCaptor.forClass(ShipPlaceRule.class);
        verify(mapper).update(cap.capture());
        assertEquals(0, cap.getValue().getState());

        ShipPlaceRule missing = in(); missing.setRuleNo(99);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(missing)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(99)).getStatusCode().value());
        service.remove(3);
        verify(mapper).delete(e);
    }

    // ── 우편번호 표 ─────────────────────────────────────────────

    @Test
    void 붙여넣은_우편번호는_줄_쉼표_공백으로_나누고_하이픈을_떼고_5_6자리_숫자만_인정한다() {
        var p = DeliRstrctZipcodeService.parse("06236\n13529, 13530;12-345  135290\nabc 1234 1234567 06236");

        assertThat(p.zipcodes()).containsExactly("06236", "13529", "13530", "12345", "135290");
        assertThat(p.invalid()).containsExactly("abc", "1234", "1234567");
        assertThat(p.duplicatedInInput()).isEqualTo(1);
        assertThat(DeliRstrctZipcodeService.parse("  ").zipcodes()).isEmpty();
        assertThat(DeliRstrctZipcodeService.parse(null).zipcodes()).isEmpty();
    }

    private final DeliRstrctZipcodeMapper zipMapper = mock(DeliRstrctZipcodeMapper.class);
    private final DeliRstrctZipcodeService zipService = new DeliRstrctZipcodeService(zipMapper);

    @Test
    void 일괄_등록은_이미_있는_우편번호를_빼고_나머지만_구분과_함께_저장한다() {
        when(zipMapper.selectExisting(eq("DeliFeeUpStar"), anyList())).thenReturn(List.of("13529"));

        var r = zipService.bulkAdd(" DeliFeeUpStar ", "06236 13529 13530 xx", "u", "사용자");

        assertEquals(2, r.added());
        assertEquals(1, r.alreadyExisted());
        assertThat(r.invalid()).containsExactly("xx");
        ArgumentCaptor<java.util.Collection<String>> cap = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(zipMapper).insertAll(eq("DeliFeeUpStar"), cap.capture(), eq("u"), eq("사용자"));
        assertThat(cap.getValue()).containsExactly("06236", "13530");
    }

    @Test
    void 일괄_등록은_구분이_없거나_형식이_틀리거나_인식된_번호가_없으면_400이다() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> zipService.bulkAdd(" ", "06236", "u", "n")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> zipService.bulkAdd("공백 있음", "06236", "u", "n")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> zipService.bulkAdd("S1", "abc", "u", "n")).getStatusCode().value());
        verify(zipMapper, never()).insertAll(anyString(), any(), any(), any());
    }

    @Test
    void 삭제는_우편번호를_하이픈_없이_정리하고_구분_전체_삭제도_한다() {
        zipService.remove("S1", "12-345");
        verify(zipMapper).deleteOne("S1", "12345");
        zipService.removeSect("S1");
        verify(zipMapper).deleteSect("S1");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> zipService.remove("S1", "abc")).getStatusCode().value());
    }

    @Test
    void 구분_목록은_표준_구분을_우편번호가_없어도_보여주고_등록된_구분과_규칙이_쓰는_구분을_합친다() {
        when(zipMapper.selectSects()).thenReturn(List.of(Map.of("sect", "DeliFeeUpStar", "cnt", 12L), Map.of("sect", "MyCustom", "cnt", 3L)));
        when(zipMapper.selectRuleSects()).thenReturn(List.of("RuleOnly", "MyCustom"));

        var list = zipService.sects();

        assertThat(list).extracting(m -> m.get("sect")).containsExactly("DeliFeeUpStar", "DeliFeeUpSquare", "DeliFeeUpDoubleStar",
                "DeliFeeUpDoubleSquare", "JerrycanLimit", "18JerrycanLimit", "MyCustom", "RuleOnly");
        assertThat(list.get(0)).containsEntry("cnt", 12).containsEntry("label", "택배비인상 ★");
        assertThat(list.get(1)).containsEntry("cnt", 0);                       // 표준 구분은 건수 0 이어도 나온다
        assertThat(list.get(6)).containsEntry("cnt", 3).containsEntry("label", "");
        assertThat(list.get(7)).containsEntry("cnt", 0);                       // 규칙만 가리키는 구분
    }

    @Test
    void 우편번호_표가_비어_있어도_표준_구분_여섯_개는_고를_수_있다() {
        when(zipMapper.selectSects()).thenReturn(List.of());
        when(zipMapper.selectRuleSects()).thenReturn(List.of());

        assertThat(zipService.sects()).hasSize(6);
    }

    // ── 배송 데이터 생성에서의 적용 ─────────────────────────────

    private final com.shopwizard.ship.mapper.ShipAutoOrderMapper autoMapper = mock(com.shopwizard.ship.mapper.ShipAutoOrderMapper.class);
    private final com.shopwizard.order.mapper.OrderGiftRuleMapper giftMapper = mock(com.shopwizard.order.mapper.OrderGiftRuleMapper.class);
    private final ShipDataBuildService build = new ShipDataBuildService(autoMapper, giftMapper, mapper);

    private static ShipPlaceRule rule(int no, String place, String prefix, String sect, String addr, String prod, String chnls) {
        ShipPlaceRule r = new ShipPlaceRule();
        r.setRuleNo(no);
        r.setShipPlace(place);
        r.setNamePrefix(prefix);
        r.setZipSect(sect);
        r.setAddrPatterns(addr);
        r.setExceptAddrPatterns("%송내동%");
        r.setProdName(prod);
        r.setExceptChnlCodes(chnls);
        return r;
    }

    @Test
    void 상점_옵션이_꺼져_있으면_규칙을_읽지도_적용하지도_않는다() {
        when(autoMapper.selectShipRstrctYn("S1")).thenReturn(0);

        var result = build.build("S1", "u", "n");

        assertThat(result.rstrctUsed()).isFalse();
        assertThat(result.placeRules()).isZero();
        verify(mapper, never()).selectActiveByShop(anyString());
        verify(autoMapper, never()).applyPlaceRule(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 상점_옵션이_켜져_있으면_사용_중_규칙을_번호_순으로_적용하고_시트별_행_수를_돌려준다() {
        when(autoMapper.selectShipRstrctYn("S1")).thenReturn(1);
        when(mapper.selectActiveByShop("S1")).thenReturn(List.of(
                rule(1, "보물유통", "", "", "", "", ""),
                rule(2, "택배비인상", "★", "DeliFeeUpStar", "%서울%강남구%\n%경기%광주%", "", "CH1, CH2"),
                rule(3, "배송불가", "", "JerrycanLimit", "", "말통", "")));
        when(autoMapper.selectPlaceCounts("S1")).thenReturn(List.of(Map.of("shipPlace", "배송불가", "cnt", 2L), Map.of("shipPlace", "보물유통", "cnt", 5L)));

        var result = build.build("S1", "u", "n");

        assertThat(result.rstrctUsed()).isTrue();
        assertThat(result.placeRules()).isEqualTo(3);
        assertThat(result.sheets()).containsEntry("배송불가", 2).containsEntry("보물유통", 5);
        var order = inOrder(autoMapper);
        order.verify(autoMapper).applyPlaceRule("S1", "보물유통", "", "", List.of(), List.of("%송내동%"), "", List.of(), "u", "n");
        order.verify(autoMapper).applyPlaceRule("S1", "택배비인상", "★", "DeliFeeUpStar", List.of("%서울%강남구%", "%경기%광주%"), List.of("%송내동%"), "", List.of("CH1", "CH2"), "u", "n");
        order.verify(autoMapper).applyPlaceRule("S1", "배송불가", "", "JerrycanLimit", List.of(), List.of("%송내동%"), "말통", List.of(), "u", "n");
        // 규칙은 묶음·출력순서보다 앞서 적용된다
        order.verify(autoMapper).resetBundle("S1");
    }

    @Test
    void 규칙_패턴_분리는_빈_줄을_버리고_공백을_자른다() {
        assertThat(ShipDataBuildService.splitLines(" %a% \n\n%b%\r\n")).containsExactly("%a%", "%b%");
        assertThat(ShipDataBuildService.splitLines(null)).isEmpty();
        assertThat(ShipDataBuildService.splitCodes("CH1, CH2 ,,")).containsExactly("CH1", "CH2");
        assertThat(ShipDataBuildService.splitCodes(" ")).isEmpty();
    }
}
