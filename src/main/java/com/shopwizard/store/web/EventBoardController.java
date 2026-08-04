package com.shopwizard.store.web;

import com.shopwizard.store.model.EventBoard;
import com.shopwizard.store.service.EventBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/eventboard")
@RequiredArgsConstructor
public class EventBoardController {
    private final EventBoardService eventBoardService;

    @GetMapping("/list")
    public List<EventBoard> selectList(@RequestParam Map<String, Object> params) {
        return eventBoardService.selectList(params);
    }

    @GetMapping("/page/list")
    public List<EventBoard> selectListPage(@RequestParam Map<String, Object> params) {
        return eventBoardService.selectListPage(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return eventBoardService.selectCount(params);
    }

    @GetMapping
    public EventBoard select(@RequestParam Map<String, Object> params) {
        return eventBoardService.select(params);
    }

    @PostMapping
    public int insert(@RequestBody EventBoard eventBoard) {
        return eventBoardService.insert(eventBoard);
    }

    @PutMapping
    public int update(@RequestBody EventBoard eventBoard) {
        return eventBoardService.update(eventBoard);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return eventBoardService.delete(params);
    }

    @DeleteMapping("/event")
    public int deleteEvent(@RequestParam Map<String, Object> params) {
        return eventBoardService.deleteEvent(params);
    }
}
