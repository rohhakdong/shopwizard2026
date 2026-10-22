package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Role;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper {
    List<Role> selectList(Map<String, Object> params);
    /** 역할 한 건(SupplyYn 포함). 없으면 null. */
    Role selectByUid(String roleUid);
    int insert(Role role);
    int update(Role role);
    int delete(String roleUid);
}
