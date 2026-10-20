package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MngrMapper;
import com.shopwizard.authority.model.Mngr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;

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
    @InjectMocks private MngrService mngrService;

    private static Mngr mngr(String uid, String passwd) {
        Mngr m = new Mngr();
        m.setMngrUid(uid);
        m.setLoginId("admin");
        m.setPasswd(passwd);
        return m;
    }

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
    void 등록과_수정은_비밀번호가_있을_때만_BCrypt로_해시한다() {
        Mngr created = mngr(null, "newpass");
        mngrService.insert(created);
        assertTrue(created.getPasswd().startsWith("$2"));
        assertTrue(ENCODER.matches("newpass", created.getPasswd()));

        Mngr changed = mngr("U1", "changed1");
        mngrService.update(changed);
        assertTrue(ENCODER.matches("changed1", changed.getPasswd()));

        Mngr untouched = mngr("U1", null);
        mngrService.update(untouched);
        assertNull(untouched.getPasswd(), "비밀번호를 안 보내면 null 그대로 — update SQL 이 Passwd 를 건드리지 않는다");

        Mngr blank = mngr("U1", "");
        mngrService.update(blank);
        assertEquals("", blank.getPasswd());
    }
}
