package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 쇼핑몰 공지(shopion.tOprNotice). */
@Mapper
public interface NoticeMapper {

    List<Notice> selectList(@Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode, @Param("title") String title,
                            @Param("content") String content, @Param("registName") String registName, @Param("status") String status,
                            @Param("start") String start, @Param("end") String end,
                            @Param("offset") int offset, @Param("limit") int limit);

    int selectCount(@Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode, @Param("title") String title,
                    @Param("content") String content, @Param("registName") String registName, @Param("status") String status,
                    @Param("start") String start, @Param("end") String end);

    Notice select(@Param("noticeNo") Integer noticeNo);

    int countChnl(@Param("chnlCode") String chnlCode);

    int countComp(@Param("compCode") String compCode);

    int insert(@Param("n") com.shopwizard.operate.service.NoticeRules.Clean n, @Param("chnlCode") String chnlCode, @Param("compCode") String compCode,
               @Param("registId") String registId, @Param("registName") String registName,
               @Param("key") com.shopwizard.operate.model.IdHolder key);

    int update(@Param("noticeNo") Integer noticeNo, @Param("n") com.shopwizard.operate.service.NoticeRules.Clean n,
               @Param("chnlCode") String chnlCode, @Param("compCode") String compCode,
               @Param("changeId") String changeId, @Param("changeName") String changeName);

    int delete(@Param("noticeNo") Integer noticeNo);
}
