package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Menu;
import com.shopwizard.authority.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/list")
    public List<Menu> selectList(@RequestParam Map<String, Object> params) {
        return menuService.selectList(params);
    }

    @GetMapping("/child-max")
    public String selectChildMax(@RequestParam Map<String, Object> params) {
        return menuService.selectChildMax(params);
    }

    @GetMapping("/list-table")
    public List<Menu> selectListTable(@RequestParam Map<String, Object> params) {
        return menuService.selectListTable(params);
    }

    @PostMapping
    public int insert(@RequestBody Menu menu) {
        return menuService.insert(menu);
    }

    @PutMapping
    public int update(@RequestBody Menu menu) {
        return menuService.update(menu);
    }

    @DeleteMapping
    public int delete(@RequestBody Map<String, Object> params) {
        return menuService.delete(params);
    }
}
