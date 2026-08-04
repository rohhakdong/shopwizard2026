package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MenuMapper;
import com.shopwizard.authority.model.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;

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
        return menuMapper.insert(menu);
    }

    @Transactional
    public int update(Menu menu) {
        return menuMapper.update(menu);
    }

    @Transactional
    public int delete(Map<String, Object> params) {
        return menuMapper.delete(params);
    }
}
