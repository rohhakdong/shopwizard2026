package com.shopwizard.log.web;

import com.shopwizard.log.model.CustInfoMngr;
import com.shopwizard.log.service.CustInfoMngrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/log/custinfomngr")
public class CustInfoMngrController {

    private final CustInfoMngrService custInfoMngrService;

    @GetMapping("/list")
    public List<CustInfoMngr> selectList(@RequestParam Map<String, Object> params) { return custInfoMngrService.selectList(params); }

    @PostMapping
    public void insert(@RequestBody CustInfoMngr custInfoMngr) { custInfoMngrService.insert(custInfoMngr); }
}
