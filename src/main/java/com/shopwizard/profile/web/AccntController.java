package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Accnt;
import com.shopwizard.profile.service.AccntService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/accnt")
public class AccntController {

    private final AccntService accntService;

    @GetMapping("/list")
    public List<Accnt> selectList(@RequestParam Map<String, Object> params) { return accntService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return accntService.selectCount(params); }

    @GetMapping("/{accntId}")
    public Accnt select(@PathVariable Integer accntId) { return accntService.select(accntId); }

    @PostMapping
    public int insert(@RequestBody Accnt accnt) { return accntService.insert(accnt); }

    @PutMapping
    public int update(@RequestBody Accnt accnt) { return accntService.update(accnt); }

    @DeleteMapping("/{accntId}")
    public int delete(@PathVariable Integer accntId) { return accntService.delete(accntId); }
}
