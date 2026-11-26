package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustInfoLogMapper;
import com.shopwizard.profile.model.CustInfoLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustInfoLogService {

    private final CustInfoLogMapper custInfoLogMapper;

    public List<CustInfoLog> selectList(Map<String, Object> params) { return custInfoLogMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custInfoLogMapper.selectCount(params); }
    public CustInfoLog select(Integer logId) { return custInfoLogMapper.select(logId); }

    /**
     * 개인정보 조회 기록 한 건 — 조회 화면이 수령인·주소·전화번호를 보여 줄 때 "누가 어디서 무엇을 조회했는가" 를 남긴다.
     * 호출한 조회와 별도 트랜잭션(REQUIRES_NEW)이라 조회가 읽기 전용이어도 기록이 남는다. 열 길이에 맞춰 자른다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String menuName, String execCntnts, String svcName, String daoName, String tableName,
                       String loginId, String name, String ip) {
        CustInfoLog log = new CustInfoLog();
        log.setCustId(0);
        log.setMenuName(cut(menuName, 255));
        log.setExecCntnts(cut(execCntnts, 512));
        log.setSvcName(cut(svcName, 255));
        log.setDaoName(cut(daoName, 255));
        log.setTableName(cut(tableName, 255));
        log.setMngrUid(cut(loginId == null ? "" : loginId, 50));
        log.setMngrLoginId(cut(loginId == null ? "" : loginId, 50));
        log.setMngrName(cut(name == null ? "" : name, 50));
        log.setMngrIp(cut(ip, 20));
        log.setState(0);
        log.setRegistId(cut(loginId, 50));
        log.setRegistName(cut(name, 50));
        custInfoLogMapper.insert(log);
    }

    static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
