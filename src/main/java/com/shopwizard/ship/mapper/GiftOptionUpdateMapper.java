package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.GiftOptionUpdate;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface GiftOptionUpdateMapper {
    List<GiftOptionUpdate> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    GiftOptionUpdate select(Map<String, Object> params);
    int insert(GiftOptionUpdate giftOptionUpdate);
    int update(GiftOptionUpdate giftOptionUpdate);
    int delete(GiftOptionUpdate giftOptionUpdate);
    /** '진행' 인 채 minutes 분이 지난 실행을 '실패' 로 돌린다. */
    int failStale(@org.apache.ibatis.annotations.Param("minutes") int minutes);
}
