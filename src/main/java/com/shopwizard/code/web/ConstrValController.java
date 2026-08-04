package com.shopwizard.code.web;

import com.shopwizard.code.model.ConstrVal;
import com.shopwizard.code.service.ConstrValService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/code/constr-val")
@RequiredArgsConstructor
public class ConstrValController {

    private final ConstrValService constrValService;

    @GetMapping
    public ResponseEntity<List<ConstrVal>> list(
            @RequestParam(defaultValue = "") String pConstrCode,
            @RequestParam(defaultValue = "") String sidx,
            @RequestParam(defaultValue = "ASC") String sord) {
        Map<String, Object> map = new HashMap<>();
        map.put("pConstrCode", pConstrCode);
        map.put("sidx", sidx);
        map.put("sord", sord);
        return ResponseEntity.ok(constrValService.findConstrValList(map));
    }

    @PostMapping
    public ResponseEntity<String> insert(@RequestBody ConstrVal constrVal) {
        constrValService.insert(constrVal);
        return ResponseEntity.ok("SUCCESS");
    }

    @PutMapping
    public ResponseEntity<String> update(@RequestBody ConstrVal constrVal) {
        constrValService.update(constrVal);
        return ResponseEntity.ok("SUCCESS");
    }

    @DeleteMapping
    public ResponseEntity<String> delete(@RequestBody ConstrVal constrVal) {
        constrValService.delete(constrVal);
        return ResponseEntity.ok("SUCCESS");
    }
}
