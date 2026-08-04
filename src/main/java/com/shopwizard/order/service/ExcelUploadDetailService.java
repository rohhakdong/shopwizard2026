package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ExcelUploadDetailMapper;
import com.shopwizard.order.model.ExcelUploadDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExcelUploadDetailService {
    private final ExcelUploadDetailMapper excelUploadDetailMapper;

    public List<ExcelUploadDetail> selectList(Map<String, Object> params) { return excelUploadDetailMapper.selectList(params); }
    public ExcelUploadDetail select(Map<String, Object> params) { return excelUploadDetailMapper.select(params); }
    public void insert(ExcelUploadDetail excelUploadDetail) { excelUploadDetailMapper.insert(excelUploadDetail); }
    public void update(ExcelUploadDetail excelUploadDetail) { excelUploadDetailMapper.update(excelUploadDetail); }
    public void delete(Map<String, Object> params) { excelUploadDetailMapper.delete(params); }
}
