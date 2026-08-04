package com.shopwizard.store.web;

import com.shopwizard.store.model.Chnl;
import com.shopwizard.store.service.ChnlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/chnl")
@RequiredArgsConstructor
public class ChnlController {
    private final ChnlService chnlService;

    @GetMapping("/list")
    public List<Chnl> selectList(@RequestParam Map<String, Object> params) {
        return chnlService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return chnlService.selectCount(params);
    }

    @GetMapping("/{chnlCode}")
    public Chnl select(@PathVariable String chnlCode) {
        return chnlService.select(chnlCode);
    }

    @GetMapping("/byname")
    public Chnl selectByName(@RequestParam String chnlName) {
        return chnlService.selectByName(chnlName);
    }

    @GetMapping("/byshoplinkermall")
    public Chnl selectByShoplinkerMall(@RequestParam Map<String, Object> params) {
        return chnlService.selectByShoplinkerMall(params);
    }

    @PostMapping
    public int insert(@RequestBody Chnl chnl) {
        return chnlService.insert(chnl);
    }

    @PutMapping
    public int update(@RequestBody Chnl chnl) {
        return chnlService.update(chnl);
    }

    @DeleteMapping
    public int delete() {
        return chnlService.delete();
    }
}
