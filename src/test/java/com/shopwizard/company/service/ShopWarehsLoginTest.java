package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.mapper.WarehsMapper;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.Warehs;
import com.shopwizard.framework.util.EncryptUtil;
import com.shopwizard.framework.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 상점·창고 계정도 신규 BCrypt, 레거시 SHA-1 은 로그인 성공 시 자동 전환. */
class ShopWarehsLoginTest {

    @Test
    @SuppressWarnings("unchecked")
    void 상점_레거시_SHA1_은_로그인_성공시_BCrypt_로_전환_틀리면_쓰지_않는다() {
        ShopMapper mapper = mock(ShopMapper.class);
        ShopService service = new ShopService(mapper);
        Shop shop = new Shop();
        shop.setShopCode("S1");
        shop.setPasswd(EncryptUtil.encryptBySHA("pw1"));
        when(mapper.selectByLoginId("id1")).thenReturn(shop);

        assertThat(service.login("id1", "bad")).isNull();
        verify(mapper, never()).updatePasswd(anyMap());

        assertThat(service.login("id1", "pw1")).isNotNull();
        org.mockito.ArgumentCaptor<Map<String, Object>> cap = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).updatePasswd(cap.capture());
        assertThat(cap.getValue()).containsEntry("shopCode", "S1");
        assertThat(PasswordHasher.isBcrypt((String) cap.getValue().get("passwd"))).isTrue();
    }

    @Test
    void 상점_BCrypt_는_BCrypt_로_검증하고_다시_저장하지_않는다_신규는_BCrypt() {
        ShopMapper mapper = mock(ShopMapper.class);
        ShopService service = new ShopService(mapper);
        Shop shop = new Shop();
        shop.setShopCode("S1");
        shop.setPasswd(PasswordHasher.hash("pw1"));
        when(mapper.selectByLoginId("id1")).thenReturn(shop);

        assertThat(service.login("id1", "pw1")).isNotNull();
        assertThat(service.login("id1", "x")).isNull();
        assertThat(service.login("id1", "")).isNull();
        verify(mapper, never()).updatePasswd(anyMap());

        Shop fresh = new Shop();
        fresh.setPasswd("newpw");
        service.insert(fresh);
        assertThat(PasswordHasher.isBcrypt(fresh.getPasswd())).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 창고_레거시_SHA1_은_로그인_성공시_BCrypt_로_전환() {
        WarehsMapper mapper = mock(WarehsMapper.class);
        WarehsService service = new WarehsService(mapper);
        Warehs w = new Warehs();
        w.setWarehsCode("W1");
        w.setPasswd(EncryptUtil.encryptBySHA("pw1"));
        when(mapper.selectByLoginId("id1")).thenReturn(w);

        assertThat(service.login("id1", "bad")).isNull();
        assertThat(service.login("id1", "pw1")).isNotNull();

        org.mockito.ArgumentCaptor<Map<String, Object>> cap = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).updatePasswd(cap.capture());
        assertThat(cap.getValue()).containsEntry("warehsCode", "W1");
        assertThat(PasswordHasher.isBcrypt((String) cap.getValue().get("passwd"))).isTrue();
    }

    @Test
    void 비밀번호_변경은_BCrypt_로_저장한다() {
        ShopMapper mapper = mock(ShopMapper.class);
        new ShopService(mapper).updatePasswd("S1", "np", "a", "b");
        @SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<Map<String, Object>> cap = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).updatePasswd(cap.capture());
        assertThat(PasswordHasher.isBcrypt((String) cap.getValue().get("passwd"))).isTrue();
    }
}
