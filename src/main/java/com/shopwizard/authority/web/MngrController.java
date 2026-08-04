package com.shopwizard.authority.web;

import com.shopwizard.authority.model.Mngr;
import com.shopwizard.authority.service.MngrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authority/mngr")
@RequiredArgsConstructor
public class MngrController {

    private final MngrService mngrService;

    @GetMapping("/list")
    public List<Mngr> selectList(@RequestParam Map<String, Object> params) {
        return mngrService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return mngrService.selectCount(params);
    }

    @GetMapping
    public Mngr select(@RequestParam Map<String, Object> params) {
        return mngrService.select(params);
    }

    @GetMapping("/info/{mngrUid}")
    public Mngr selectInfo(@PathVariable String mngrUid) {
        return mngrService.selectInfo(mngrUid);
    }

    @GetMapping("/prod-approv-yn/{loginId}")
    public int selectProdApprovYn(@PathVariable String loginId) {
        return mngrService.selectProdApprovYn(loginId);
    }

    @PostMapping
    public int insert(@RequestBody Mngr mngr) {
        return mngrService.insert(mngr);
    }

    @PutMapping
    public int update(@RequestBody Mngr mngr) {
        return mngrService.update(mngr);
    }

    @DeleteMapping("/{mngrUid}")
    public int delete(@PathVariable String mngrUid) {
        return mngrService.delete(mngrUid);
    }
}
