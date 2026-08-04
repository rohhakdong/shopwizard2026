package com.shopwizard.message.service;

import com.shopwizard.message.mapper.TermnlMapper;
import com.shopwizard.message.model.Termnl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TermnlService {

    private final TermnlMapper termnlMapper;

    public List<Termnl> selectList(Map<String, Object> params) { return termnlMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return termnlMapper.selectCount(params); }
    public Termnl select(Map<String, Object> params) { return termnlMapper.select(params); }
    public void insert(Termnl termnl) { termnlMapper.insert(termnl); }
    public void update(Termnl termnl) { termnlMapper.update(termnl); }
    public void delete(Termnl termnl) { termnlMapper.delete(termnl); }
}
