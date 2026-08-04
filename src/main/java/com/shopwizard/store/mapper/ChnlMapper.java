package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Chnl;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChnlMapper {
    List<Chnl> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Chnl select(String chnlCode);
    Chnl selectByName(String chnlName);
    Chnl selectByShoplinkerMall(Map<String, Object> params);
    int insert(Chnl chnl);
    int update(Chnl chnl);
    int delete();
}
