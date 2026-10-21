package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.FunctnMapper;
import com.shopwizard.authority.model.Functn;
import com.shopwizard.framework.auth.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FunctnService {

    private final FunctnMapper functnMapper;
    private final RolePermissionService rolePermissionService;   // 메뉴/기능이 바뀌면 역할 권한 캐시를 비운다

    public List<Functn> selectList(Map<String, Object> params) {
        return functnMapper.selectList(params);
    }

    public boolean selectAuth(Map<String, Object> params) {
        return functnMapper.selectAuth(params);
    }

    public Functn select(Map<String, Object> params) {
        return functnMapper.select(params);
    }

    @Transactional
    public int insert(Functn functn) {
        int n = functnMapper.insert(functn);
        rolePermissionService.invalidate();
        return n;
    }

    @Transactional
    public int update(Functn functn) {
        int n = functnMapper.update(functn);
        rolePermissionService.invalidate();
        return n;
    }

    @Transactional
    public int delete(Functn functn) {
        int n = functnMapper.delete(functn);
        rolePermissionService.invalidate();
        return n;
    }
}
