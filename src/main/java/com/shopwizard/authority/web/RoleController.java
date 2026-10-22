package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Role;
import com.shopwizard.authority.service.RoleService;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    private final DataScopeGuard scopeGuard;

    @GetMapping("/list")
    public List<Role> selectList(@RequestParam Map<String, Object> params) {
        return roleService.selectList(params);
    }

    @PostMapping
    public int insert(@RequestBody Role role, HttpServletRequest request) {
        role.setSvcCode(scopeGuard.svcFor(request, role.getSvcCode()));
        return roleService.insert(role);
    }

    @PutMapping
    public int update(@RequestBody Role role, HttpServletRequest request) {
        scopeGuard.requireRoleSvc(request, role.getRoleUid());
        role.setSvcCode(scopeGuard.svcFor(request, role.getSvcCode()));
        return roleService.update(role);
    }

    @DeleteMapping("/{roleUid}")
    public int delete(@PathVariable String roleUid, HttpServletRequest request) {
        scopeGuard.requireRoleSvc(request, roleUid);
        return roleService.delete(roleUid);
    }
}
