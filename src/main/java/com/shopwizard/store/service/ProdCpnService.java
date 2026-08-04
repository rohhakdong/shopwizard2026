package com.shopwizard.store.service;

import com.shopwizard.store.mapper.ProdCpnMapper;
import com.shopwizard.store.model.ProdCpn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdCpnService {
    private final ProdCpnMapper prodCpnMapper;

    public List<ProdCpn> selectList(Map<String, Object> params) { return prodCpnMapper.selectList(params); }
    public ProdCpn select(Integer cpnId) { return prodCpnMapper.select(cpnId); }
    public int insert(ProdCpn prodCpn) { return prodCpnMapper.insert(prodCpn); }
    public int update(ProdCpn prodCpn) { return prodCpnMapper.update(prodCpn); }
    public int delete(Integer cpnId) { return prodCpnMapper.delete(cpnId); }
}
