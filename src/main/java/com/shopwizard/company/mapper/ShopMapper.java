package com.shopwizard.company.mapper;

import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShopMapper {
    List<Shop> selectList(Map<String, Object> params);
    List<ShopPublicView> selectPublicList();
    int selectCount(Map<String, Object> params);
    String selectMax(String supplyCode);
    Shop select(String shopCode);
    Shop selectByName(String shopName);
    int selectLoginIdCount(Map<String, Object> params);
    int insert(Shop shop);
    int update(Shop shop);
    int delete(Shop shop);
}
