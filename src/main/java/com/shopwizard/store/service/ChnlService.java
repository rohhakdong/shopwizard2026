package com.shopwizard.store.service;

import com.shopwizard.store.mapper.ChnlMapper;
import com.shopwizard.store.model.Chnl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ChnlService {
    private final ChnlMapper chnlMapper;

    public List<Chnl> selectList(Map<String, Object> params) { return chnlMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return chnlMapper.selectCount(params); }
    public Chnl select(String chnlCode) { return chnlMapper.select(chnlCode); }
    public Chnl selectByName(String chnlName) { return chnlMapper.selectByName(chnlName); }
    public Chnl selectByShoplinkerMall(Map<String, Object> params) { return chnlMapper.selectByShoplinkerMall(params); }
    public int insert(Chnl chnl) { return chnlMapper.insert(chnl); }
    public int update(Chnl chnl) { return chnlMapper.update(chnl); }
    public int delete() { return chnlMapper.delete(); }
}
