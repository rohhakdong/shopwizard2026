package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.ProdCodeMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdCodeMatchService {
    private final ProdCodeMatchMapper prodCodeMatchMapper;
    public List<ProdCodeMatch> selectList(Map<String, Object> params) { return prodCodeMatchMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodCodeMatchMapper.selectCount(params); }
    public ProdCodeMatch select(Map<String, Object> params) { return prodCodeMatchMapper.select(params); }
    public int insert(ProdCodeMatch prodCodeMatch) { return prodCodeMatchMapper.insert(prodCodeMatch); }
    public int update(ProdCodeMatch prodCodeMatch) { return prodCodeMatchMapper.update(prodCodeMatch); }
    public int delete(ProdCodeMatch prodCodeMatch) { return prodCodeMatchMapper.delete(prodCodeMatch); }
}
