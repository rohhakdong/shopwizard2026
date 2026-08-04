package com.shopwizard.code.mapper;

import com.shopwizard.code.model.WordDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WordDictMapper {
    List<WordDict> findList(Map<String, Object> map);
    String findAbbr(@Param("korWord") String korWord);
    int countByKorWord(@Param("korWord") String korWord);
    int insert(WordDict wordDict);
    int update(WordDict wordDict);
    int delete(@Param("korWord") String korWord);
}
