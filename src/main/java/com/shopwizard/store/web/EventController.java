package com.shopwizard.store.web;

import com.shopwizard.store.model.Event;
import com.shopwizard.store.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping("/list")
    public List<Event> selectList(@RequestParam Map<String, Object> params) {
        return eventService.selectList(params);
    }

    @GetMapping("/level/list")
    public List<Event> selectListLevel(@RequestParam Map<String, Object> params) {
        return eventService.selectListLevel(params);
    }

    @GetMapping("/codemax")
    public String selectEventCodeMax(@RequestParam Map<String, Object> params) {
        return eventService.selectEventCodeMax(params);
    }

    @GetMapping
    public Event select(@RequestParam Map<String, Object> params) {
        return eventService.select(params);
    }

    @GetMapping("/html")
    public String selectEventHtml(@RequestParam Map<String, Object> params) {
        return eventService.selectEventHtml(params);
    }

    @PostMapping
    public int insert(@RequestBody Event event) {
        return eventService.insert(event);
    }

    @PutMapping
    public int update(@RequestBody Event event) {
        return eventService.update(event);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return eventService.delete(params);
    }
}
