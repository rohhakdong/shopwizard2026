package com.shopwizard.code.service;

import com.shopwizard.code.mapper.ConstrMapper;
import com.shopwizard.code.model.Constr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConstrService {

    private final ConstrMapper constrMapper;

    public List<Constr> findConstrList(Map<String, Object> map) {
        return constrMapper.findList(map);
    }

    @Transactional
    public void insert(Constr constr) {
        constrMapper.insert(constr);
    }

    @Transactional
    public void update(Constr constr) {
        constrMapper.update(constr);
    }

    @Transactional
    public void delete(String constrCode) {
        constrMapper.delete(constrCode);
    }

    @Transactional
    public void deleteMultiple(List<Constr> list) {
        constrMapper.deleteMultiple(list);
    }
}
