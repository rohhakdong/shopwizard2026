package com.shopwizard.code.web;

import com.shopwizard.code.model.Model;
import com.shopwizard.code.model.ModelAttr;
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

    @GetMapping("/attr/list")
    public List<ModelAttr> selectAttrList(@RequestParam Map<String, Object> params) { return modelService.selectAttrList(params); }

}
