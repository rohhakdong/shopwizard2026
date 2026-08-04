package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Role;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper {
    List<Role> selectList(Map<String, Object> params);
    int insert(Role role);
    int update(Role role);
    int delete(String roleUid);
}
