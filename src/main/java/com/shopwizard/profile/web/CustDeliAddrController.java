package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CustDeliAddr;
import com.shopwizard.profile.service.CustDeliAddrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/custDeliAddr")
public class CustDeliAddrController {

    private final CustDeliAddrService custDeliAddrService;

    @GetMapping("/list")
    public List<CustDeliAddr> selectList(@RequestParam Map<String, Object> params) { return custDeliAddrService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custDeliAddrService.selectCount(params); }

    @GetMapping
    public CustDeliAddr select(@RequestParam Map<String, Object> params) { return custDeliAddrService.select(params); }

    @GetMapping("/default/{custId}")
    public CustDeliAddr selectDefault(@PathVariable Integer custId) { return custDeliAddrService.selectDefault(custId); }

}
