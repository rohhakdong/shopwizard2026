package com.shopwizard.message.web;

import com.shopwizard.message.model.SendRecevLog;
import com.shopwizard.message.service.SendRecevLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/message/sendrecevlog")
public class SendRecevLogController {

    private final SendRecevLogService sendRecevLogService;

    @GetMapping("/list")
    public List<SendRecevLog> selectList(@RequestParam Map<String, Object> params) { return sendRecevLogService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return sendRecevLogService.selectCount(params); }

    @GetMapping
    public SendRecevLog select(@RequestParam Map<String, Object> params) { return sendRecevLogService.select(params); }

    @GetMapping("/top/{docType}")
    public SendRecevLog selectTop(@PathVariable String docType) { return sendRecevLogService.selectTop(docType); }

}
