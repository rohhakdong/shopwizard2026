package com.shopwizard.code.web;

import com.shopwizard.code.model.Zipcode4;
import com.shopwizard.code.service.Zipcode4Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/code/zipcode")
@RequiredArgsConstructor
public class Zipcode4Controller {

    private final Zipcode4Service zipcode4Service;

    @GetMapping
    public ResponseEntity<List<Zipcode4>> list(@RequestParam String keyword) {
        return ResponseEntity.ok(zipcode4Service.findZipcodeList(keyword));
    }
}
