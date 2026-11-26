package com.shopwizard.company.mapper;

import com.shopwizard.company.model.SupplyCompNotice;
import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.service.NoticeRules;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 공급사 공지(shopwizard.tCmpSupplyCompNotice). */
@Mapper
public interface SupplyCompNoticeMapper {

    List<SupplyCompNotice> selectList(@Param("title") String title, @Param("status") String status,
                                      @Param("start") String start, @Param("end") String end,
                                      @Param("offset") int offset, @Param("limit") int limit);

    int selectCount(@Param("title") String title, @Param("status") String status,
                    @Param("start") String start, @Param("end") String end);

    SupplyCompNotice select(@Param("noticeNo") Integer noticeNo);

    int insert(@Param("n") NoticeRules.Clean n, @Param("registId") String registId, @Param("registName") String registName,
               @Param("key") IdHolder key);

    int update(@Param("noticeNo") Integer noticeNo, @Param("n") NoticeRules.Clean n,
               @Param("changeId") String changeId, @Param("changeName") String changeName);

    int delete(@Param("noticeNo") Integer noticeNo);
}
