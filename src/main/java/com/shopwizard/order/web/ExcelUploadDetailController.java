package com.shopwizard.order.web;

import com.shopwizard.order.model.ExcelUploadDetail;
import com.shopwizard.order.service.ExcelUploadDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/exceluploaddetail")
public class ExcelUploadDetailController {
    private final ExcelUploadDetailService excelUploadDetailService;

    @GetMapping("/list")
    public List<ExcelUploadDetail> selectList(@RequestParam Map<String, Object> params) { return excelUploadDetailService.selectList(params); }

    @GetMapping
    public ExcelUploadDetail select(@RequestParam Map<String, Object> params) { return excelUploadDetailService.select(params); }

    @PostMapping
    public void insert(@RequestBody ExcelUploadDetail excelUploadDetail) { excelUploadDetailService.insert(excelUploadDetail); }

    @PutMapping
    public void update(@RequestBody ExcelUploadDetail excelUploadDetail) { excelUploadDetailService.update(excelUploadDetail); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { excelUploadDetailService.delete(params); }
}
