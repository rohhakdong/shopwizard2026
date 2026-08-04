package com.shopwizard.company.mapper;

import com.shopwizard.company.model.ChnlPolicy;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChnlPolicyMapper {
    List<ChnlPolicy> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ChnlPolicy select(Map<String, Object> params);
    String selectMax();
    int insert(ChnlPolicy chnlPolicy);
    int update(ChnlPolicy chnlPolicy);
    int delete(ChnlPolicy chnlPolicy);
}
