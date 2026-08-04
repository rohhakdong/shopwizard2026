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

    @PostMapping
    public void insert(@RequestBody Mantnc mantnc) { mantncService.insert(mantnc); }

    @PutMapping
    public void update(@RequestBody Mantnc mantnc) { mantncService.update(mantnc); }

    @PutMapping("/cmpletconfrm")
    public void updateByCmpletConfrm(@RequestBody Mantnc mantnc) { mantncService.updateByCmpletConfrm(mantnc); }

    @PutMapping("/reciptconfrm")
    public void updateByReciptConfrm(@RequestBody Mantnc mantnc) { mantncService.updateByReciptConfrm(mantnc); }

    @PutMapping("/exec")
    public void updateByExec(@RequestBody Mantnc mantnc) { mantncService.updateByExec(mantnc); }

    @PutMapping("/cmplet")
    public void updateByCmplet(@RequestBody Mantnc mantnc) { mantncService.updateByCmplet(mantnc); }

    @DeleteMapping
    public void delete(@RequestBody Mantnc mantnc) { mantncService.delete(mantnc); }
}
