package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.FunctnMapper;
import com.shopwizard.authority.mapper.MenuMapper;
import com.shopwizard.authority.model.Functn;
import com.shopwizard.authority.model.Menu;
import com.shopwizard.framework.auth.RolePermissionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 메뉴/기능을 바꾸면 역할 권한 캐시를 즉시 비워서 다음 요청부터 새 권한이 적용된다. */
class PermissionCacheInvalidationTest {

    private final RolePermissionService permissions = mock(RolePermissionService.class);
    private final MenuService menuService = new MenuService(mock(MenuMapper.class), permissions);
    private final FunctnService functnService = new FunctnService(mock(FunctnMapper.class), permissions);

    @Test
    void 메뉴_변경은_캐시를_비운다() {
        menuService.insert(new Menu());
        menuService.update(new Menu());
        menuService.delete(Map.of());
        org.mockito.Mockito.verify(permissions, org.mockito.Mockito.times(3)).invalidate();
    }

    @Test
    void 기능_변경은_캐시를_비운다() {
        functnService.insert(new Functn());
        functnService.update(new Functn());
        functnService.delete(new Functn());
        verify(permissions, org.mockito.Mockito.times(3)).invalidate();
    }
}
