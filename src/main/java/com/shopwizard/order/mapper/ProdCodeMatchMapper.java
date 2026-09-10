package com.shopwizard.order.mapper;

import com.shopwizard.order.model.ProdCodeMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProdCodeMatchMapper {
    List<ProdCodeMatch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdCodeMatch select(Map<String, Object> params);
    int insert(ProdCodeMatch prodCodeMatch);
    int update(ProdCodeMatch prodCodeMatch);
    int delete(ProdCodeMatch prodCodeMatch);

    /** 실제 PK 는 (ProdName, ProdOption) 복합키. 상품명+옵션으로 매칭 규칙 1건 조회. */
    ProdCodeMatch selectByNameOption(@Param("prodName") String prodName, @Param("prodOption") String prodOption);

    /** 매칭 규칙 등록/갱신 (ProdName+ProdOption 기준 upsert). */
    void upsert(ProdCodeMatch prodCodeMatch);
}
