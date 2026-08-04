package com.shopwizard.order.web;

import com.shopwizard.order.model.ExcelUpload;
import com.shopwizard.order.service.ExcelUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/excelupload")
public class ExcelUploadController {
    private final ExcelUploadService excelUploadService;

    @GetMapping("/list")
    public List<ExcelUpload> selectList(@RequestParam Map<String, Object> params) { return excelUploadService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return excelUploadService.selectCount(params); }

    @GetMapping
    public ExcelUpload select(@RequestParam Map<String, Object> params) { return excelUploadService.select(params); }

    @PostMapping
    public void insert(@RequestBody ExcelUpload excelUpload) { excelUploadService.insert(excelUpload); }

    @PutMapping
    public void update(@RequestBody ExcelUpload excelUpload) { excelUploadService.update(excelUpload); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { excelUploadService.delete(params); }
}
