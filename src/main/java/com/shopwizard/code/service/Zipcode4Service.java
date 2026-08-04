package com.shopwizard.code.service;

import com.shopwizard.code.mapper.Zipcode4Mapper;
import com.shopwizard.code.model.Zipcode4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Zipcode4Service {

    private final Zipcode4Mapper zipcode4Mapper;

    public List<Zipcode4> findZipcodeList(String keyword) {
        return zipcode4Mapper.findList(keyword);
    }
}
