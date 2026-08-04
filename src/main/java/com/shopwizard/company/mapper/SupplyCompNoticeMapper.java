package com.shopwizard.company.mapper;

import com.shopwizard.company.model.SupplyCompNotice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyCompNoticeMapper {
    List<SupplyCompNotice> selectList(Map<String, Object> params);
    SupplyCompNotice select(Map<String, Object> params);
    String selectCntnts(int noticeNo);
    int insert(SupplyCompNotice supplyCompNotice);
    int update(SupplyCompNotice supplyCompNotice);
    int delete(SupplyCompNotice supplyCompNotice);
}
