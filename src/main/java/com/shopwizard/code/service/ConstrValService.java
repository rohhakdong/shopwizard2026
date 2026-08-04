package com.shopwizard.code.service;

import com.shopwizard.code.mapper.ConstrValMapper;
import com.shopwizard.code.model.ConstrVal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConstrValService {

    private final ConstrValMapper constrValMapper;

    public List<ConstrVal> findConstrValList(Map<String, Object> map) {
        return constrValMapper.findList(map);
    }

    @Transactional
    public void insert(ConstrVal constrVal) {
        constrValMapper.insert(constrVal);
    }

    @Transactional
    public void update(ConstrVal constrVal) {
        constrValMapper.update(constrVal);
    }

    @Transactional
    public void delete(ConstrVal constrVal) {
        constrValMapper.delete(constrVal);
    }
}
