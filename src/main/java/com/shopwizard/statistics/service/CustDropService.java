package com.shopwizard.statistics.service;

import com.shopwizard.statistics.mapper.CustDropMapper;
import com.shopwizard.statistics.model.CustDrop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustDropService {

    private final CustDropMapper custDropMapper;

    public void insert(CustDrop custDrop) { custDropMapper.insert(custDrop); }
}
