package com.shopwizard.operate.web;

import com.shopwizard.operate.model.Faq;
import com.shopwizard.operate.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/faq")
public class FaqController {
    private final FaqService faqService;

    @GetMapping("/list")
    public List<Faq> selectList(@RequestParam Map<String, Object> params) { return faqService.selectList(params); }

    @GetMapping("/{faqNo}")
    public Faq select(@PathVariable Integer faqNo) { return faqService.select(faqNo); }

    @GetMapping("/cntnts/{faqNo}")
    public String selectFaqCntnts(@PathVariable Integer faqNo) { return faqService.selectFaqCntnts(faqNo); }
}
