package com.shopwizard.code.service;

import com.shopwizard.code.mapper.WordDictMapper;
import com.shopwizard.code.model.WordDict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WordDictService {

    private final WordDictMapper wordDictMapper;

    public List<WordDict> findWordDictList(Map<String, Object> map) {
        return wordDictMapper.findList(map);
    }

}
