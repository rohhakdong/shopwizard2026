package com.shopwizard.code.mapper;

import com.shopwizard.code.model.Model;
import com.shopwizard.code.model.WordDict;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ModelMapper {
    List<Model> selectList(Map<String, Object> params);
    Model select(Integer modelId);
    int insert(Model model);
    int update(Model model);
    int delete(Integer modelId);
    int updateWordDict1(WordDict wordDict);
    int updateWordDict2(WordDict wordDict);
    int updateWordDict3(WordDict wordDict);
    int updateWordDict4(WordDict wordDict);
    int updateWordDict5(WordDict wordDict);
}
