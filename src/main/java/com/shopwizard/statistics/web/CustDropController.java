package com.shopwizard.statistics.web;

import com.shopwizard.statistics.model.CustDrop;
import com.shopwizard.statistics.service.CustDropService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/statistics/custdrop")
public class CustDropController {

    private final CustDropService custDropService;

    @PostMapping
    public void insert(@RequestBody CustDrop custDrop) { custDropService.insert(custDrop); }
}
