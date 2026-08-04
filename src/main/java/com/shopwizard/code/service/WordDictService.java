package com.shopwizard.code.service;

import com.shopwizard.code.mapper.WordDictMapper;
import com.shopwizard.code.model.WordDict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WordDictService {

    private final WordDictMapper wordDictMapper;

    public List<WordDict> findWordDictList(Map<String, Object> map) {
        return wordDictMapper.findList(map);
    }

    @Transactional
    public void insert(WordDict wordDict) {
        wordDictMapper.insert(wordDict);
    }

    @Transactional
    public void update(WordDict wordDict) {
        wordDictMapper.update(wordDict);
    }

    @Transactional
    public void delete(String korWord) {
        wordDictMapper.delete(korWord);
    }
}
