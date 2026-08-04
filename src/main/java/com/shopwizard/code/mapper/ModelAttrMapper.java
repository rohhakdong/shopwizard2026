package com.shopwizard.code.mapper;

import com.shopwizard.code.model.ModelAttr;
import com.shopwizard.code.model.WordDict;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ModelAttrMapper {
    List<ModelAttr> selectList(Map<String, Object> params);
    int insert(ModelAttr modelAttr);
    int update(ModelAttr modelAttr);
    int delete(ModelAttr modelAttr);
    int updateWordDict1(WordDict wordDict);
    int updateWordDict2(WordDict wordDict);
    int updateWordDict3(WordDict wordDict);
    int updateWordDict4(WordDict wordDict);
    int updateWordDict5(WordDict wordDict);
}
