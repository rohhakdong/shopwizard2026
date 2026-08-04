package com.shopwizard.operate.web;

import com.shopwizard.operate.model.CustConslt;
import com.shopwizard.operate.model.CustConsltDashBoard;
import com.shopwizard.operate.service.CustConsltService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/custconslt")
public class CustConsltController {
    private final CustConsltService custConsltService;

    @GetMapping("/list")
    public List<CustConslt> selectList(@RequestParam Map<String, Object> params) { return custConsltService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custConsltService.selectCount(params); }

    @GetMapping("/{custConsltNo}")
    public CustConslt select(@PathVariable Integer custConsltNo) { return custConsltService.select(custConsltNo); }

    @PostMapping
    public void insert(@RequestBody CustConslt custConslt) { custConsltService.insert(custConslt); }

    @PutMapping
    public void update(@RequestBody CustConslt custConslt) { custConsltService.update(custConslt); }

    @PutMapping("/move")
    public void move(@RequestBody CustConslt custConslt) { custConsltService.move(custConslt); }

    @DeleteMapping("/{custConsltNo}")
    public void delete(@PathVariable Integer custConsltNo) { custConsltService.delete(custConsltNo); }

    @GetMapping("/count/cust/{custId}")
    public int countByCustId(@PathVariable Integer custId) { return custConsltService.countByCustId(custId); }

    @GetMapping("/dashboard")
    public List<CustConsltDashBoard> selectListDashboard(@RequestParam Map<String, Object> params) { return custConsltService.selectListDashboard(params); }
}
