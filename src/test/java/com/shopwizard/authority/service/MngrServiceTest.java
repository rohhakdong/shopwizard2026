package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MngrMapper;
import com.shopwizard.authority.mapper.RoleMapper;
import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.model.Role;
import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.Shop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MngrServiceTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock private MngrMapper mngrMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private ShopMapper shopMapper;
    @InjectMocks private MngrService mngrService;

    /** 사용 중(State=1)인 일반 계정. */
    private static Mngr mngr(String uid, String passwd) {
        Mngr m = new Mngr();
        m.setMngrUid(uid);
        m.setLoginId("admin");
        m.setPasswd(passwd);
        m.setState(1);
        return m;
    }

    private static Role role(String uid, int supplyYn) {
        Role r = new Role();
        r.setRoleUid(uid);
        r.setSupplyYn(supplyYn);
        return r;
    }

    private void givenRole(String uid, int supplyYn) {
        when(roleMapper.selectByUid(uid)).thenReturn(role(uid, supplyYn));
    }

    private void givenShopExists(String code) {
        when(shopMapper.select(code)).thenReturn(new Shop());
    }

    // ───────────────────────── 로그인 ─────────────────────────

    @Test
    void 빈_비밀번호나_빈_아이디는_DB조회도_없이_실패한다() {
        assertNull(mngrService.login("admin", "", null));
        assertNull(mngrService.login("admin", null, null));
        assertNull(mngrService.login("", "x", null));
        assertNull(mngrService.login(null, "x", null));

        verifyNoInteractions(mngrMapper);
    }

    @Test
    void 평문_저장값이_일치하면_로그인되고_BCrypt로_전환_저장된다() {
        Mngr stored = mngr("U1", "secret12");
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(stored));

        Mngr result = mngrService.login("admin", "secret12", null);

        assertSame(stored, result);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(mngrMapper).updatePasswd(eq("U1"), hash.capture());
        assertTrue(hash.getValue().startsWith("$2"), "BCrypt 형식으로 저장");
        assertTrue(ENCODER.matches("secret12", hash.getValue()));
    }

    @Test
    void 평문_저장값과_다르면_실패하고_저장도_안한다() {
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(mngr("U1", "secret12")));

        assertNull(mngrService.login("admin", "secret13", null));
        assertNull(mngrService.login("admin", "SECRET12", null));

        verify(mngrMapper, never()).updatePasswd(anyString(), anyString());
    }

    @Test
    void BCrypt_저장값은_BCrypt로_검증하고_다시_저장하지_않는다() {
        Mngr stored = mngr("U1", ENCODER.encode("secret12"));
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(stored));

        assertSame(stored, mngrService.login("admin", "secret12", null));
        assertNull(mngrService.login("admin", "wrong", null));

        verify(mngrMapper, never()).updatePasswd(anyString(), anyString());
    }

    @Test
    void 저장된_비밀번호가_없는_계정은_로그인할_수_없다() {
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(mngr("U1", null), mngr("U2", "")));

        assertNull(mngrService.login("admin", "anything", null));
    }

    @Test
    void 채널코드가_있으면_조회_조건으로_전달한다() {
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of());

        mngrService.login("admin", "x", "CH1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(mngrMapper).selectForLogin(params.capture());
        assertEquals("admin", params.getValue().get("pLoginId"));
        assertEquals("CH1", params.getValue().get("pChnlCode"));
    }

    @Test
    void 미사용_계정은_비밀번호가_맞아도_로그인되지_않고_비밀번호_전환도_하지_않는다() {
        Mngr off = mngr("U1", "secret12");
        off.setState(0);
        Mngr noState = mngr("U2", "secret12");
        noState.setState(null);
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(off, noState));

        assertNull(mngrService.login("admin", "secret12", null));

        verify(mngrMapper, never()).updatePasswd(anyString(), anyString());
    }

    @Test
    void 업체_역할_계정인데_상점이_없으면_자격이_맞아도_403으로_거부한다() {
        Mngr supplier = mngr("U1", ENCODER.encode("secret12"));
        supplier.setSupplyYn(1);
        supplier.setShopCode("  ");   // 공백도 없는 것으로 본다
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(supplier));

        assertThatThrownBy(() -> mngrService.login("admin", "secret12", null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(403);
                    assertThat(e.getReason()).contains("상점이 지정되지 않은 업체 계정");
                });
    }

    @Test
    void 업체_역할이라도_비밀번호가_틀리면_상점_유무를_알려주지_않는다() {
        Mngr supplier = mngr("U1", ENCODER.encode("secret12"));
        supplier.setSupplyYn(1);
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(supplier));

        assertNull(mngrService.login("admin", "wrong", null));
    }

    @Test
    void 업체_역할_계정이_상점을_가지면_정상_로그인된다() {
        Mngr supplier = mngr("U1", ENCODER.encode("secret12"));
        supplier.setSupplyYn(1);
        supplier.setShopCode("S1");
        when(mngrMapper.selectForLogin(any())).thenReturn(List.of(supplier));

        assertSame(supplier, mngrService.login("admin", "secret12", null));
    }

    // ───────────────────────── 등록/수정 ─────────────────────────

    @Test
    void 등록과_수정은_비밀번호가_있을_때만_BCrypt로_해시한다() {
        givenRole("R1", 0);
        Mngr created = mngr(null, "newpass");
        created.setRoleUid("R1");
        mngrService.insert(created);
        assertTrue(created.getPasswd().startsWith("$2"));
        assertTrue(ENCODER.matches("newpass", created.getPasswd()));

        Mngr changed = mngr("U1", "changed1");
        changed.setRoleUid("R1");
        mngrService.update(changed);
        assertTrue(ENCODER.matches("changed1", changed.getPasswd()));

        Mngr untouched = mngr("U1", null);
        untouched.setRoleUid("R1");
        mngrService.update(untouched);
        assertNull(untouched.getPasswd(), "비밀번호를 안 보내면 null 그대로 — update SQL 이 Passwd 를 건드리지 않는다");

        Mngr blank = mngr("U1", "");
        blank.setRoleUid("R1");
        mngrService.update(blank);
        assertEquals("", blank.getPasswd());
    }

    @Test
    void 업체_역할_계정은_상점_없이_등록할_수_없다() {
        givenRole("SUP", 1);
        Mngr m = mngr(null, "pw");
        m.setRoleUid("SUP");

        assertThatThrownBy(() -> mngrService.insert(m))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(400);
                    assertThat(e.getReason()).contains("업체 역할 계정은 상점을 지정해야 합니다");
                });
        m.setShopCode("   ");
        assertThatThrownBy(() -> mngrService.insert(m)).isInstanceOf(ResponseStatusException.class);
        verify(mngrMapper, never()).insert(any());
    }

    @Test
    void 업체_역할_계정은_존재하는_상점을_지정하면_등록된다_상점코드는_trim() {
        givenRole("SUP", 1);
        givenShopExists("S1");
        Mngr m = mngr(null, "pw");
        m.setRoleUid("SUP");
        m.setShopCode(" S1 ");

        mngrService.insert(m);

        assertEquals("S1", m.getShopCode());
        verify(mngrMapper).insert(m);
    }

    @Test
    void 등록하면_MngrUid_는_서버가_발급한다_클라이언트_값은_믿지_않는다() {
        givenRole("R1", 0);
        Mngr m = mngr("FORGED-UID", "pw");
        m.setRoleUid("R1");

        mngrService.insert(m);

        assertThat(m.getMngrUid()).isNotEqualTo("FORGED-UID").matches("[0-9a-f-]{36}");
        verify(mngrMapper).insert(m);
    }

    @Test
    void 존재하지_않는_상점코드는_역할과_무관하게_거부한다() {
        givenRole("R1", 0);
        when(shopMapper.select("NOPE")).thenReturn(null);
        Mngr m = mngr(null, "pw");
        m.setRoleUid("R1");
        m.setShopCode("NOPE");

        assertThatThrownBy(() -> mngrService.insert(m))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("존재하지 않는 상점코드"));
    }

    @Test
    void 일반_역할_계정은_상점이_없어도_된다_빈_값은_null_로_저장() {
        givenRole("R1", 0);
        Mngr m = mngr(null, "pw");
        m.setRoleUid("R1");
        m.setShopCode("");

        mngrService.insert(m);

        assertNull(m.getShopCode());
        verify(mngrMapper).insert(m);
    }

    @Test
    void 존재하지_않는_역할은_거부한다() {
        when(roleMapper.selectByUid("GONE")).thenReturn(null);
        Mngr m = mngr(null, "pw");
        m.setRoleUid("GONE");

        assertThatThrownBy(() -> mngrService.insert(m))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("존재하지 않는 역할"));
    }

    @Test
    void 비활성_계정은_업체_역할이어도_상점_없이_저장할_수_있다() {
        givenRole("SUP", 1);
        Mngr m = mngr(null, "pw");
        m.setRoleUid("SUP");
        m.setState(0);

        mngrService.insert(m);

        verify(mngrMapper).insert(m);
    }

    @Test
    void 수정_요청에_상점이_없으면_현재_상점으로_판단하고_기존값을_유지한다() {
        Mngr current = mngr("U1", null);
        current.setRoleUid("SUP");
        current.setShopCode("S1");
        when(mngrMapper.selectInfo("U1")).thenReturn(current);
        givenRole("SUP", 1);
        givenShopExists("S1");
        Mngr req = mngr("U1", null);
        req.setRoleUid("SUP");           // shopCode 는 안 보냄(null)

        mngrService.update(req);

        assertNull(req.getShopCode(), "null 그대로 — 매퍼가 ShopCode 를 건드리지 않는다");
        verify(mngrMapper).update(req);
    }

    @Test
    void 수정에서_상점을_비우면_업체_역할은_거부된다() {
        Mngr current = mngr("U1", null);
        current.setRoleUid("SUP");
        current.setShopCode("S1");
        when(mngrMapper.selectInfo("U1")).thenReturn(current);
        givenRole("SUP", 1);
        Mngr req = mngr("U1", null);
        req.setRoleUid("SUP");
        req.setShopCode("");             // 해제 시도

        assertThatThrownBy(() -> mngrService.update(req)).isInstanceOf(ResponseStatusException.class);
        verify(mngrMapper, never()).update(any());
    }

    @Test
    void 역할을_업체_역할로_바꾸는데_현재_상점도_없으면_거부된다() {
        Mngr current = mngr("U1", null);
        current.setRoleUid("GENERAL");
        when(mngrMapper.selectInfo("U1")).thenReturn(current);
        givenRole("SUP", 1);
        Mngr req = mngr("U1", null);
        req.setRoleUid("SUP");

        assertThatThrownBy(() -> mngrService.update(req))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("업체 역할 계정은 상점을 지정해야 합니다"));
    }

    @Test
    void 미사용_업체_계정을_사용으로_켜려면_상점이_있어야_한다() {
        Mngr current = mngr("U1", null);
        current.setRoleUid("SUP");
        current.setState(0);
        when(mngrMapper.selectInfo("U1")).thenReturn(current);
        givenRole("SUP", 1);
        Mngr req = mngr("U1", null);
        req.setRoleUid("SUP");
        req.setState(1);

        assertThatThrownBy(() -> mngrService.update(req)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 수정에서_상점을_바꾸면_trim_하고_존재를_확인한다() {
        Mngr current = mngr("U1", null);
        current.setRoleUid("SUP");
        current.setShopCode("S1");
        when(mngrMapper.selectInfo("U1")).thenReturn(current);
        givenRole("SUP", 1);
        givenShopExists("S2");
        Mngr req = mngr("U1", null);
        req.setRoleUid("SUP");
        req.setShopCode(" S2 ");

        mngrService.update(req);

        assertEquals("S2", req.getShopCode());
        verify(mngrMapper).update(req);
    }
}
