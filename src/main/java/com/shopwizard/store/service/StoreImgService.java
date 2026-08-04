package com.shopwizard.store.service;

import com.shopwizard.store.mapper.StoreImgMapper;
import com.shopwizard.store.model.StoreImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreImgService {
    private final StoreImgMapper storeImgMapper;

    public List<StoreImg> selectList(Map<String, Object> params) { return storeImgMapper.selectList(params); }
    public StoreImg select(Map<String, Object> params) { return storeImgMapper.select(params); }
    public String selectStoreImgHtml(Map<String, Object> params) { return storeImgMapper.selectStoreImgHtml(params); }
    public int insert(StoreImg storeImg) { return storeImgMapper.insert(storeImg); }
    public int update(StoreImg storeImg) { return storeImgMapper.update(storeImg); }
    public int delete(Map<String, Object> params) { return storeImgMapper.delete(params); }
}
