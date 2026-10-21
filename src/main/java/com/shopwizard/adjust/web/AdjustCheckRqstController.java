package com.shopwizard.adjust.web;

import com.shopwizard.adjust.model.AdjustCheckRqst;
import com.shopwizard.adjust.service.AdjustCheckRqstService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/adjust/checkreqst")
public class AdjustCheckRqstController {

    private final AdjustCheckRqstService adjustCheckRqstService;

    @GetMapping("/list")
    public List<AdjustCheckRqst> selectList(@RequestParam Map<String, Object> params) { return adjustCheckRqstService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return adjustCheckRqstService.selectCount(params); }

    @GetMapping
    public AdjustCheckRqst select(@RequestParam Map<String, Object> params) { return adjustCheckRqstService.select(params); }

}
