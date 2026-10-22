package com.shopwizard.operate.web;

import com.shopwizard.operate.model.Agree;
import com.shopwizard.operate.service.AgreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/agree")
public class AgreeController {
    private final AgreeService agreeService;

    @GetMapping("/list")
    public List<Agree> selectList(@RequestParam Map<String, Object> params) { return agreeService.selectList(params); }

    @GetMapping
    public Agree select(@RequestParam Map<String, Object> params) { return agreeService.select(params); }

    @GetMapping("/cntnts/{agreeNo}")
    public String selectCntnts(@PathVariable Integer agreeNo) { return agreeService.selectCntnts(agreeNo); }
}
