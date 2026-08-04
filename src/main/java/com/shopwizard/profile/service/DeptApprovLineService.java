package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DeptApprovLineMapper;
import com.shopwizard.profile.model.DeptApprovLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DeptApprovLineService {

    private final DeptApprovLineMapper deptApprovLineMapper;

    public List<DeptApprovLine> selectList(Map<String, Object> params) { return deptApprovLineMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return deptApprovLineMapper.selectCount(params); }
    public DeptApprovLine select(Map<String, Object> params) { return deptApprovLineMapper.select(params); }
    public int insert(DeptApprovLine deptApprovLine) { return deptApprovLineMapper.insert(deptApprovLine); }
    public int update(DeptApprovLine deptApprovLine) { return deptApprovLineMapper.update(deptApprovLine); }
    public int delete(Map<String, Object> params) { return deptApprovLineMapper.delete(params); }
}
