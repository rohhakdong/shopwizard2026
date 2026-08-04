package com.shopwizard.order.mapper;

import com.shopwizard.order.model.ExcelUploadDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExcelUploadDetailMapper {
    List<ExcelUploadDetail> selectList(Map<String, Object> params);
    ExcelUploadDetail select(Map<String, Object> params);
    void insert(ExcelUploadDetail excelUploadDetail);
    void update(ExcelUploadDetail excelUploadDetail);
    void delete(Map<String, Object> params);
}
