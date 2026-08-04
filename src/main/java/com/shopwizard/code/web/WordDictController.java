package com.shopwizard.code.web;

import com.shopwizard.code.model.WordDict;
import com.shopwizard.code.service.WordDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/code/word-dict")
@RequiredArgsConstructor
public class WordDictController {

    private final WordDictService wordDictService;

    @GetMapping
    public ResponseEntity<List<WordDict>> list(
            @RequestParam(defaultValue = "") String pKorWord,
            @RequestParam(defaultValue = "") String sidx,
            @RequestParam(defaultValue = "ASC") String sord) {
        Map<String, Object> map = new HashMap<>();
        map.put("pKorWord", pKorWord);
        map.put("sidx", sidx);
        map.put("sord", sord);
        return ResponseEntity.ok(wordDictService.findWordDictList(map));
    }

    @PostMapping
    public ResponseEntity<String> insert(@RequestBody WordDict wordDict) {
        wordDictService.insert(wordDict);
        return ResponseEntity.ok("SUCCESS");
    }

    @PutMapping("/{korWord}")
    public ResponseEntity<String> update(@PathVariable String korWord,
                                         @RequestBody WordDict wordDict) {
        wordDict.setKorWord(korWord);
        wordDictService.update(wordDict);
        return ResponseEntity.ok("SUCCESS");
    }

    @DeleteMapping("/{korWord}")
    public ResponseEntity<String> delete(@PathVariable String korWord) {
        wordDictService.delete(korWord);
        return ResponseEntity.ok("SUCCESS");
    }
}
