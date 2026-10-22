package com.shopwizard.store.web;

import com.shopwizard.store.model.CustCpn;
import com.shopwizard.store.service.CustCpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/custcpn")
@RequiredArgsConstructor
public class CustCpnController {
    private final CustCpnService custCpnService;

    @GetMapping("/list")
    public List<CustCpn> selectList(@RequestParam Map<String, Object> params) {
        return custCpnService.selectList(params);
    }

    @GetMapping("/{cpnId}")
    public CustCpn select(@PathVariable Integer cpnId) {
        return custCpnService.select(cpnId);
    }

}
