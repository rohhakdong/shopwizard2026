package com.shopwizard.code.web;

import com.shopwizard.code.model.Constr;
import com.shopwizard.code.service.ConstrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/code/constr")
@RequiredArgsConstructor
public class ConstrController {

    private final ConstrService constrService;

    @GetMapping
    public ResponseEntity<List<Constr>> list(
            @RequestParam(defaultValue = "") String pConstrName,
            @RequestParam(defaultValue = "") String sidx,
            @RequestParam(defaultValue = "ASC") String sord) {
        Map<String, Object> map = new HashMap<>();
        map.put("pConstrName", pConstrName);
        map.put("sidx", sidx);
        map.put("sord", sord);
        return ResponseEntity.ok(constrService.findConstrList(map));
    }

    @PostMapping
    public ResponseEntity<String> insert(@RequestBody Constr constr) {
        constrService.insert(constr);
        return ResponseEntity.ok("SUCCESS");
    }

    @PutMapping("/{constrCode}")
    public ResponseEntity<String> update(@PathVariable String constrCode,
                                         @RequestBody Constr constr) {
        constr.setConstrCode(constrCode);
        constrService.update(constr);
        return ResponseEntity.ok("SUCCESS");
    }

    @DeleteMapping("/{constrCode}")
    public ResponseEntity<String> delete(@PathVariable String constrCode) {
        constrService.delete(constrCode);
        return ResponseEntity.ok("SUCCESS");
    }

    @DeleteMapping("/multiple")
    public ResponseEntity<String> deleteMultiple(@RequestBody List<Constr> list) {
        constrService.deleteMultiple(list);
        return ResponseEntity.ok("SUCCESS");
    }
}
