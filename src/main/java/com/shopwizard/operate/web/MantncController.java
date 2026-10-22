package com.shopwizard.operate.web;

import com.shopwizard.operate.model.Mantnc;
import com.shopwizard.operate.service.MantncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/mantnc")
public class MantncController {

    private final MantncService mantncService;

    @GetMapping("/list")
    public List<Mantnc> selectList(@RequestParam Map<String, Object> params) { return mantncService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return mantncService.selectCount(params); }

    @GetMapping
    public Mantnc select(@RequestParam Map<String, Object> params) { return mantncService.select(params); }

    @GetMapping("/max")
    public int selectMax(@RequestParam Map<String, Object> params) { return mantncService.selectMax(params); }

}
