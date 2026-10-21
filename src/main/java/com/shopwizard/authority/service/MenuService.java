package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MenuMapper;
import com.shopwizard.authority.model.Menu;
import com.shopwizard.framework.auth.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final RolePermissionService rolePermissionService;   // 메뉴/기능이 바뀌면 역할 권한 캐시를 비운다

    public List<Menu> selectList(Map<String, Object> params) {
        return menuMapper.selectList(params);
    }

    public String selectChildMax(Map<String, Object> params) {
        return menuMapper.selectChildMax(params);
    }

    public List<Menu> selectListTable(Map<String, Object> params) {
        return menuMapper.selectListTable(params);
    }

    @Transactional
    public int insert(Menu menu) {
        int n = menuMapper.insert(menu);
        rolePermissionService.invalidate();
        return n;
    }

    @Transactional
    public int update(Menu menu) {
        int n = menuMapper.update(menu);
        rolePermissionService.invalidate();
        return n;
    }

    @Transactional
    public int delete(Map<String, Object> params) {
        int n = menuMapper.delete(params);
        rolePermissionService.invalidate();
        return n;
    }
}
