package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ProdOptionChangeMapper;
import com.shopwizard.order.model.ProdOptionChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdOptionChangeService {
    private final ProdOptionChangeMapper prodOptionChangeMapper;
    public List<ProdOptionChange> selectList(Map<String, Object> params) { return prodOptionChangeMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodOptionChangeMapper.selectCount(params); }
    public ProdOptionChange select(Map<String, Object> params) { return prodOptionChangeMapper.select(params); }
    public int insert(ProdOptionChange prodOptionChange) { return prodOptionChangeMapper.insert(prodOptionChange); }
    public int update(ProdOptionChange prodOptionChange) { return prodOptionChangeMapper.update(prodOptionChange); }
    public int delete(ProdOptionChange prodOptionChange) { return prodOptionChangeMapper.delete(prodOptionChange); }
}
