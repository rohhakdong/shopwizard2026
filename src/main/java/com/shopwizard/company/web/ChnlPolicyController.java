package com.shopwizard.company.web;

import com.shopwizard.company.model.ChnlPolicy;
import com.shopwizard.company.service.ChnlPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/chnl-policy")
@RequiredArgsConstructor
public class ChnlPolicyController {
    private final ChnlPolicyService chnlPolicyService;

    @GetMapping
    public List<ChnlPolicy> getList(@RequestParam Map<String, Object> params) {
        return chnlPolicyService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return chnlPolicyService.getCount(params);
    }

    @GetMapping("/{chnlPolicyCode}")
    public ChnlPolicy get(@PathVariable String chnlPolicyCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("chnlPolicyCode", chnlPolicyCode);
        return chnlPolicyService.get(params);
    }

    @GetMapping("/max")
    public String getMax() {
        return chnlPolicyService.getMax();
    }

    @PostMapping
    public int insert(@RequestBody ChnlPolicy chnlPolicy) {
        return chnlPolicyService.insert(chnlPolicy);
    }

    @PutMapping
    public int update(@RequestBody ChnlPolicy chnlPolicy) {
        return chnlPolicyService.update(chnlPolicy);
    }

    @DeleteMapping
    public int delete(@RequestBody ChnlPolicy chnlPolicy) {
        return chnlPolicyService.delete(chnlPolicy);
    }
}
