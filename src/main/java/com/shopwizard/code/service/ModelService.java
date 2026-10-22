package com.shopwizard.code.service;

import com.shopwizard.code.mapper.ModelAttrMapper;
import com.shopwizard.code.mapper.ModelMapper;
import com.shopwizard.code.model.Model;
import com.shopwizard.code.model.ModelAttr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ModelService {

    private final ModelMapper modelMapper;
    private final ModelAttrMapper modelAttrMapper;

    public List<Model> selectList(Map<String, Object> params) { return modelMapper.selectList(params); }
    public Model select(Integer modelId) { return modelMapper.select(modelId); }

    public List<ModelAttr> selectAttrList(Map<String, Object> params) { return modelAttrMapper.selectList(params); }
}
