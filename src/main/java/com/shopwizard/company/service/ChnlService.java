package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ChnlMapper;
import com.shopwizard.company.model.Chnl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service("companyChnlService")
@RequiredArgsConstructor
public class ChnlService {
    private final ChnlMapper chnlMapper;

    public List<Chnl> getList(Map<String, Object> params) { return chnlMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return chnlMapper.selectCount(params); }
    public Chnl get(Map<String, Object> params) { return chnlMapper.select(params); }
    public String getMax(String saleCompCode) { return chnlMapper.selectMax(saleCompCode); }

    @Transactional
    public int insert(Chnl chnl) {
        chnlMapper.insertShopion(chnl);
        return chnlMapper.insert(chnl);
    }

    @Transactional
    public int update(Chnl chnl) {
        chnlMapper.updateShopion(chnl);
        return chnlMapper.update(chnl);
    }

    @Transactional
    public int delete(Chnl chnl) {
        chnlMapper.deleteShopion(chnl);
        return chnlMapper.delete(chnl);
    }
}
