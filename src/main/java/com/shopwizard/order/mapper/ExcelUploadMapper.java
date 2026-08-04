package com.shopwizard.order.mapper;

import com.shopwizard.order.model.ExcelUpload;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExcelUploadMapper {
    List<ExcelUpload> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ExcelUpload select(Map<String, Object> params);
    void insert(ExcelUpload excelUpload);
    void update(ExcelUpload excelUpload);
    void delete(Map<String, Object> params);
}
