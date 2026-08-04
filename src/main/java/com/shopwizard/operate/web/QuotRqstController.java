package com.shopwizard.operate.web;

import com.shopwizard.operate.model.QuotRqst;
import com.shopwizard.operate.service.QuotRqstService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/quotrqst")
public class QuotRqstController {
    private final QuotRqstService quotRqstService;

    @GetMapping("/list")
    public List<QuotRqst> selectList(@RequestParam Map<String, Object> params) { return quotRqstService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return quotRqstService.selectCount(params); }

    @GetMapping
    public QuotRqst select(@RequestParam Map<String, Object> params) { return quotRqstService.select(params); }

    @PostMapping
    public void insert(@RequestBody QuotRqst quotRqst) { quotRqstService.insert(quotRqst); }

    @PutMapping
    public void update(@RequestBody QuotRqst quotRqst) { quotRqstService.update(quotRqst); }

    @DeleteMapping
    public void delete(@RequestBody QuotRqst quotRqst) { quotRqstService.delete(quotRqst); }
}
