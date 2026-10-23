package com.shopwizard.profile.service;

import com.shopwizard.framework.util.EncryptUtil;
import com.shopwizard.framework.util.PasswordHasher;
import com.shopwizard.profile.mapper.CustMapper;
import com.shopwizard.profile.model.Cust;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 고객 비밀번호: 새 값은 BCrypt, 레거시 SHA-1/MD5 는 로그인 성공 시 BCrypt 로 자동 전환, 일반 수정은 비밀번호를 건드리지 않는다. */
@ExtendWith(MockitoExtension.class)
class CustServiceTest {

    @Mock private CustMapper custMapper;
    @InjectMocks private CustService service;

    private static Cust stored(String passwd) {
        Cust c = new Cust();
        c.setCustId(7);
        c.setLoginId("u1");
        c.setPasswd(passwd);
        return c;
    }

    @Test
    void 신규_회원_비밀번호는_BCrypt_로_저장한다() {
        Cust c = new Cust();
        c.setPasswd("pw-1234");

        service.insert(c);

        ArgumentCaptor<Cust> cap = ArgumentCaptor.forClass(Cust.class);
        verify(custMapper).insert(cap.capture());
        assertThat(PasswordHasher.isBcrypt(cap.getValue().getPasswd())).isTrue();
        assertThat(cap.getValue().getPasswd()).isNotEqualTo("pw-1234");
    }

    @Test
    void 비어_있거나_너무_긴_비밀번호는_저장하지_않는다() {
        Cust empty = new Cust();
        assertThatThrownBy(() -> service.insert(empty)).isInstanceOf(ResponseStatusException.class);
        Cust blank = new Cust(); blank.setPasswd("");
        assertThatThrownBy(() -> service.updatePasswd(blank)).isInstanceOf(ResponseStatusException.class);
        Cust longPw = new Cust(); longPw.setPasswd("x".repeat(73));
        assertThatThrownBy(() -> service.insert(longPw)).isInstanceOf(ResponseStatusException.class);
        verify(custMapper, never()).insert(any());
        verify(custMapper, never()).updatePasswd(any());
    }

    @Test
    void 비밀번호_재설정도_BCrypt_로_저장한다() {
        Cust c = new Cust();
        c.setCustId(7);
        c.setPasswd("new-pw");

        service.updatePasswd(c);

        ArgumentCaptor<Cust> cap = ArgumentCaptor.forClass(Cust.class);
        verify(custMapper).updatePasswd(cap.capture());
        assertThat(PasswordHasher.isBcrypt(cap.getValue().getPasswd())).isTrue();
    }

    @Test
    void 일반_수정은_바디의_비밀번호를_버린다() {
        Cust c = new Cust();
        c.setCustId(7);
        c.setPasswd("plain-from-client");

        service.update(c);

        ArgumentCaptor<Cust> cap = ArgumentCaptor.forClass(Cust.class);
        verify(custMapper).update(cap.capture());
        assertThat(cap.getValue().getPasswd()).isNull();   // 매퍼는 passwd 가 비어 있으면 Passwd 컬럼을 건드리지 않는다
    }

    @Test
    void 레거시_SHA1_고객은_로그인에_성공하면_BCrypt_로_전환된다() {
        when(custMapper.selectByLoginId(any())).thenReturn(stored(EncryptUtil.encryptBySHA("secret1")));

        Cust c = service.login("u1", "secret1", "", false);

        assertThat(c).isNotNull();
        ArgumentCaptor<Cust> cap = ArgumentCaptor.forClass(Cust.class);
        verify(custMapper).updatePasswd(cap.capture());
        assertThat(cap.getValue().getCustId()).isEqualTo(7);
        assertThat(PasswordHasher.isBcrypt(cap.getValue().getPasswd())).isTrue();
        verify(custMapper).updateLastLoginTime(any());
    }

    @Test
    void 레거시_MD5_고객도_로그인에_성공하면_BCrypt_로_전환된다() {
        when(custMapper.selectByLoginId(any())).thenReturn(stored(EncryptUtil.encryptByMD5WithPHP("secret1")));

        assertThat(service.login("u1", "secret1", "", true)).isNotNull();

        ArgumentCaptor<Cust> cap = ArgumentCaptor.forClass(Cust.class);
        verify(custMapper).updatePasswd(cap.capture());
        assertThat(PasswordHasher.isBcrypt(cap.getValue().getPasswd())).isTrue();
    }

    @Test
    void 이미_BCrypt_인_고객은_BCrypt_로_검증하고_다시_저장하지_않는다() {
        when(custMapper.selectByLoginId(any())).thenReturn(stored(PasswordHasher.hash("secret1")));

        assertThat(service.login("u1", "secret1", "", false)).isNotNull();
        assertThat(service.login("u1", "wrong", "", false)).isNull();

        verify(custMapper, never()).updatePasswd(any());
    }

    @Test
    void 비밀번호가_틀리거나_비었거나_없는_계정이면_실패하고_아무것도_쓰지_않는다() {
        when(custMapper.selectByLoginId(any())).thenReturn(stored(EncryptUtil.encryptBySHA("secret1")), (Cust) null);

        assertThat(service.login("u1", "wrong", "", false)).isNull();
        assertThat(service.login("nobody", "secret1", "", false)).isNull();

        verify(custMapper, never()).updatePasswd(any());
        verify(custMapper, never()).updateLastLoginTime(any());
    }

    @Test
    void 빈_비밀번호는_어떤_저장값에도_통과하지_않는다() {
        when(custMapper.selectByLoginId(any())).thenReturn(stored(EncryptUtil.encryptBySHA("")));

        assertThat(service.login("u1", "", "", false)).isNull();
        assertThat(service.login("u1", null, "", false)).isNull();
    }

    @Test
    void 해시를_만들_수_없는_긴_비밀번호는_전환만_건너뛰고_로그인은_허용한다() {
        String longPw = "y".repeat(80);
        when(custMapper.selectByLoginId(any())).thenReturn(stored(EncryptUtil.encryptBySHA(longPw)));

        assertThat(service.login("u1", longPw, "", false)).isNotNull();

        verify(custMapper, never()).updatePasswd(any());   // 72바이트 초과 → 전환 불가, 레거시 해시 유지
    }
}
