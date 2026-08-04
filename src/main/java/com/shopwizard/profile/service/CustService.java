package com.shopwizard.profile.service;

import com.shopwizard.framework.util.EncryptUtil;
import com.shopwizard.profile.mapper.CustMapper;
import com.shopwizard.profile.model.Cust;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustService {

    private final CustMapper custMapper;

    public List<Cust> selectList(Map<String, Object> params) { return custMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custMapper.selectCount(params); }
    public List<Cust> selectListEmp(Map<String, Object> params) { return custMapper.selectListEmp(params); }
    public Cust select(Integer custId) { return custMapper.select(custId); }
    public Cust selectByLoginId(Map<String, Object> params) { return custMapper.selectByLoginId(params); }
    public Cust selectForIdpw(Map<String, Object> params) { return custMapper.selectForIdpw(params); }
    public int insert(Cust cust) { return custMapper.insert(cust); }
    public int update(Cust cust) { return custMapper.update(cust); }
    public int updatePasswd(Cust cust) { return custMapper.updatePasswd(cust); }
    public int updateLastLoginTime(Cust cust) { return custMapper.updateLastLoginTime(cust); }
    public int updateInfoOrder(Map<String, Object> params) { return custMapper.updateInfoOrder(params); }
    public int updateInfoOrderExcel(Map<String, Object> params) { return custMapper.updateInfoOrderExcel(params); }
    public int updateInfoOrderShoplinker(Map<String, Object> params) { return custMapper.updateInfoOrderShoplinker(params); }
    public int updateInfoShipDirect(Map<String, Object> params) { return custMapper.updateInfoShipDirect(params); }
    public int updateInfoShipDirectPrint(Map<String, Object> params) { return custMapper.updateInfoShipDirectPrint(params); }
    public int updateInfoReturnDirect(Map<String, Object> params) { return custMapper.updateInfoReturnDirect(params); }
    public int updateInfoOrderAll(Map<String, Object> params) { return custMapper.updateInfoOrderAll(params); }
    public int updateInfoOrderExcelAll(Map<String, Object> params) { return custMapper.updateInfoOrderExcelAll(params); }
    public int updateInfoOrderShoplinkerAll(Map<String, Object> params) { return custMapper.updateInfoOrderShoplinkerAll(params); }
    public int updateInfoShipDirectAll(Map<String, Object> params) { return custMapper.updateInfoShipDirectAll(params); }
    public int updateInfoShipDirectPrintAll(Map<String, Object> params) { return custMapper.updateInfoShipDirectPrintAll(params); }
    public int updateInfoReturnDirectAll(Map<String, Object> params) { return custMapper.updateInfoReturnDirectAll(params); }
    public int delete(Integer custId) { return custMapper.delete(custId); }

    public Cust login(String loginId, String rawPassword, String chnlCode, boolean useMd5) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginId", loginId);
        params.put("memberChnlCode", chnlCode);
        Cust cust = custMapper.selectByLoginId(params);
        if (cust == null) return null;

        String hashed = useMd5
                ? EncryptUtil.encryptByMD5WithPHP(rawPassword)
                : EncryptUtil.encryptBySHA(rawPassword);
        if (!hashed.equals(cust.getPasswd())) return null;

        custMapper.updateLastLoginTime(cust);
        return cust;
    }
}
