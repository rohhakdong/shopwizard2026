package com.shopwizard.code.web;

import com.shopwizard.code.model.Model;
import com.shopwizard.code.model.ModelAttr;
import com.shopwizard.code.model.WordDict;
import com.shopwizard.code.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/code/model")
public class ModelController {

    private final ModelService modelService;

    @GetMapping("/list")
    public List<Model> selectList(@RequestParam Map<String, Object> params) { return modelService.selectList(params); }

    @GetMapping("/{modelId}")
    public Model select(@PathVariable Integer modelId) { return modelService.select(modelId); }

    @PostMapping
    public void insert(@RequestBody Model model) { modelService.insert(model); }

    @PutMapping
    public void update(@RequestBody Model model) { modelService.update(model); }

    @DeleteMapping("/{modelId}")
    public void delete(@PathVariable Integer modelId) { modelService.delete(modelId); }

    @PutMapping("/worddict/1")
    public void updateWordDict1(@RequestBody WordDict wordDict) { modelService.updateWordDict1(wordDict); }

    @PutMapping("/worddict/2")
    public void updateWordDict2(@RequestBody WordDict wordDict) { modelService.updateWordDict2(wordDict); }

    @PutMapping("/worddict/3")
    public void updateWordDict3(@RequestBody WordDict wordDict) { modelService.updateWordDict3(wordDict); }

    @PutMapping("/worddict/4")
    public void updateWordDict4(@RequestBody WordDict wordDict) { modelService.updateWordDict4(wordDict); }

    @PutMapping("/worddict/5")
    public void updateWordDict5(@RequestBody WordDict wordDict) { modelService.updateWordDict5(wordDict); }

    @GetMapping("/attr/list")
    public List<ModelAttr> selectAttrList(@RequestParam Map<String, Object> params) { return modelService.selectAttrList(params); }

    @PostMapping("/attr")
    public void insertAttr(@RequestBody ModelAttr modelAttr) { modelService.insertAttr(modelAttr); }

    @PutMapping("/attr")
    public void updateAttr(@RequestBody ModelAttr modelAttr) { modelService.updateAttr(modelAttr); }

    @DeleteMapping("/attr")
    public void deleteAttr(@RequestBody ModelAttr modelAttr) { modelService.deleteAttr(modelAttr); }
}
