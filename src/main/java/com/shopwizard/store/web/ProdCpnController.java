package com.shopwizard.store.web;

import com.shopwizard.store.model.ProdCpn;
import com.shopwizard.store.service.ProdCpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/prodcpn")
@RequiredArgsConstructor
public class ProdCpnController {
    private final ProdCpnService prodCpnService;

    @GetMapping("/list")
    public List<ProdCpn> selectList(@RequestParam Map<String, Object> params) {
        return prodCpnService.selectList(params);
    }

    @GetMapping("/{cpnId}")
    public ProdCpn select(@PathVariable Integer cpnId) {
        return prodCpnService.select(cpnId);
    }

    @PostMapping
    public int insert(@RequestBody ProdCpn prodCpn) {
        return prodCpnService.insert(prodCpn);
    }

    @PutMapping
    public int update(@RequestBody ProdCpn prodCpn) {
        return prodCpnService.update(prodCpn);
    }

    @DeleteMapping("/{cpnId}")
    public int delete(@PathVariable Integer cpnId) {
        return prodCpnService.delete(cpnId);
    }
}
