package com.shopwizard.company.mapper;

import com.shopwizard.company.model.Chnl;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("companyChnlMapper")
public interface ChnlMapper {
    List<Chnl> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Chnl select(Map<String, Object> params);
    String selectMax(String saleCompCode);
    int insert(Chnl chnl);
    int insertShopion(Chnl chnl);
    int update(Chnl chnl);
    int updateShopion(Chnl chnl);
    int delete(Chnl chnl);
    int deleteShopion(Chnl chnl);
}
