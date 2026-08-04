package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.RoleMapper;
import com.shopwizard.authority.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<Role> selectList(Map<String, Object> params) {
        return roleMapper.selectList(params);
    }

    @Transactional
    public int insert(Role role) {
        return roleMapper.insert(role);
    }

    @Transactional
    public int update(Role role) {
        return roleMapper.update(role);
    }

    @Transactional
    public int delete(String roleUid) {
        return roleMapper.delete(roleUid);
    }
}
