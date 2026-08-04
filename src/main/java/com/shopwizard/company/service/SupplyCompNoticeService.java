package com.shopwizard.company.service;

import com.shopwizard.company.mapper.SupplyCompNoticeMapper;
import com.shopwizard.company.model.SupplyCompNotice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplyCompNoticeService {
    private final SupplyCompNoticeMapper supplyCompNoticeMapper;

    public List<SupplyCompNotice> getList(Map<String, Object> params) { return supplyCompNoticeMapper.selectList(params); }
    public SupplyCompNotice get(Map<String, Object> params) { return supplyCompNoticeMapper.select(params); }
    public String getCntnts(int noticeNo) { return supplyCompNoticeMapper.selectCntnts(noticeNo); }

    @Transactional
    public int insert(SupplyCompNotice supplyCompNotice) { return supplyCompNoticeMapper.insert(supplyCompNotice); }
    @Transactional
    public int update(SupplyCompNotice supplyCompNotice) { return supplyCompNoticeMapper.update(supplyCompNotice); }
    @Transactional
    public int delete(SupplyCompNotice supplyCompNotice) { return supplyCompNoticeMapper.delete(supplyCompNotice); }
}
