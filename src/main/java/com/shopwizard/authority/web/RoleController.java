package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Role;
import com.shopwizard.authority.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public List<Role> selectList(@RequestParam Map<String, Object> params) {
        return roleService.selectList(params);
    }

    @PostMapping
    public int insert(@RequestBody Role role) {
        return roleService.insert(role);
    }

    @PutMapping
    public int update(@RequestBody Role role) {
        return roleService.update(role);
    }

    @DeleteMapping("/{roleUid}")
    public int delete(@PathVariable String roleUid) {
        return roleService.delete(roleUid);
    }
}
