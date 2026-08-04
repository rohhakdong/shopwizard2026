package com.shopwizard.code.service;

import com.shopwizard.code.mapper.ModelAttrMapper;
import com.shopwizard.code.mapper.ModelMapper;
import com.shopwizard.code.model.Model;
import com.shopwizard.code.model.ModelAttr;
import com.shopwizard.code.model.WordDict;
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
    public void insert(Model model) { modelMapper.insert(model); }
    public void update(Model model) { modelMapper.update(model); }
    public void delete(Integer modelId) { modelMapper.delete(modelId); }
    public void updateWordDict1(WordDict wordDict) { modelMapper.updateWordDict1(wordDict); modelAttrMapper.updateWordDict1(wordDict); }
    public void updateWordDict2(WordDict wordDict) { modelMapper.updateWordDict2(wordDict); modelAttrMapper.updateWordDict2(wordDict); }
    public void updateWordDict3(WordDict wordDict) { modelMapper.updateWordDict3(wordDict); modelAttrMapper.updateWordDict3(wordDict); }
    public void updateWordDict4(WordDict wordDict) { modelMapper.updateWordDict4(wordDict); modelAttrMapper.updateWordDict4(wordDict); }
    public void updateWordDict5(WordDict wordDict) { modelMapper.updateWordDict5(wordDict); modelAttrMapper.updateWordDict5(wordDict); }

    public List<ModelAttr> selectAttrList(Map<String, Object> params) { return modelAttrMapper.selectList(params); }
    public void insertAttr(ModelAttr modelAttr) { modelAttrMapper.insert(modelAttr); }
    public void updateAttr(ModelAttr modelAttr) { modelAttrMapper.update(modelAttr); }
    public void deleteAttr(ModelAttr modelAttr) { modelAttrMapper.delete(modelAttr); }
}
