package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.MngrMapper;
import com.shopwizard.authority.model.Mngr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MngrService {

    private final MngrMapper mngrMapper;

    public List<Mngr> selectList(Map<String, Object> params) {
        return mngrMapper.selectList(params);
    }

    public int selectCount(Map<String, Object> params) {
        return mngrMapper.selectCount(params);
    }

    public Mngr select(Map<String, Object> params) {
        return mngrMapper.select(params);
    }

    public Mngr selectInfo(String mngrUid) {
        return mngrMapper.selectInfo(mngrUid);
    }

    public int selectProdApprovYn(String loginId) {
        return mngrMapper.selectProdApprovYn(loginId);
    }

    public Mngr login(String loginId, String passwd, String chnlCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("pLoginId", loginId);
        map.put("pPasswd", passwd);
        map.put("pChnlCode", chnlCode);
        return mngrMapper.select(map);
    }

    @Transactional
    public int insert(Mngr mngr) {
        return mngrMapper.insert(mngr);
    }

    @Transactional
    public int update(Mngr mngr) {
        return mngrMapper.update(mngr);
    }

    @Transactional
    public int delete(String mngrUid) {
        return mngrMapper.delete(mngrUid);
    }
}
