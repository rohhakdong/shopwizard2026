package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ExcelUploadMapper;
import com.shopwizard.order.model.ExcelUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExcelUploadService {
    private final ExcelUploadMapper excelUploadMapper;

    public List<ExcelUpload> selectList(Map<String, Object> params) { return excelUploadMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return excelUploadMapper.selectCount(params); }
    public ExcelUpload select(Map<String, Object> params) { return excelUploadMapper.select(params); }
    public void insert(ExcelUpload excelUpload) { excelUploadMapper.insert(excelUpload); }
    public void update(ExcelUpload excelUpload) { excelUploadMapper.update(excelUpload); }
    public void delete(Map<String, Object> params) { excelUploadMapper.delete(params); }
}
