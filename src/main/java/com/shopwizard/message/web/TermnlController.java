package com.shopwizard.message.web;

import com.shopwizard.message.model.Termnl;
import com.shopwizard.message.service.TermnlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/message/termnl")
public class TermnlController {

    private final TermnlService termnlService;

    @GetMapping("/list")
    public List<Termnl> selectList(@RequestParam Map<String, Object> params) { return termnlService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return termnlService.selectCount(params); }

    @GetMapping
    public Termnl select(@RequestParam Map<String, Object> params) { return termnlService.select(params); }

    @PostMapping
    public void insert(@RequestBody Termnl termnl) { termnlService.insert(termnl); }

    @PutMapping
    public void update(@RequestBody Termnl termnl) { termnlService.update(termnl); }

    @DeleteMapping
    public void delete(@RequestBody Termnl termnl) { termnlService.delete(termnl); }
}
