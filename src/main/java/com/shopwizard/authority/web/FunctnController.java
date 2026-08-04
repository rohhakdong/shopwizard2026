package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Functn;
import com.shopwizard.authority.service.FunctnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/functn")
@RequiredArgsConstructor
public class FunctnController {

    private final FunctnService functnService;

    @GetMapping("/list")
    public List<Functn> selectList(@RequestParam Map<String, Object> params) {
        return functnService.selectList(params);
    }

    @GetMapping("/auth")
    public boolean selectAuth(@RequestParam Map<String, Object> params) {
        return functnService.selectAuth(params);
    }

    @GetMapping
    public Functn select(@RequestParam Map<String, Object> params) {
        return functnService.select(params);
    }

    @PostMapping
    public int insert(@RequestBody Functn functn) {
        return functnService.insert(functn);
    }

    @PutMapping
    public int update(@RequestBody Functn functn) {
        return functnService.update(functn);
    }

    @DeleteMapping
    public int delete(@RequestBody Functn functn) {
        return functnService.delete(functn);
    }
}
