package com.shopwizard.operate.web;

import com.shopwizard.operate.model.Notice;
import com.shopwizard.operate.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/notice")
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping("/list")
    public List<Notice> selectList(@RequestParam Map<String, Object> params) { return noticeService.selectList(params); }

    @GetMapping("/{noticeNo}")
    public Notice select(@PathVariable Integer noticeNo) { return noticeService.select(noticeNo); }

    @PostMapping
    public void insert(@RequestBody Notice notice) { noticeService.insert(notice); }

    @PutMapping
    public void update(@RequestBody Notice notice) { noticeService.update(notice); }

    @DeleteMapping("/{noticeNo}")
    public void delete(@PathVariable Integer noticeNo) { noticeService.delete(noticeNo); }

    @GetMapping("/page/list")
    public List<Notice> selectListPage(@RequestParam Map<String, Object> params) { return noticeService.selectListPage(params); }

    @GetMapping("/page/count")
    public int selectCountPage(@RequestParam Map<String, Object> params) { return noticeService.selectCountPage(params); }

    @GetMapping("/cntnts/{noticeNo}")
    public String selectCntnts(@PathVariable Integer noticeNo) { return noticeService.selectCntnts(noticeNo); }
}
