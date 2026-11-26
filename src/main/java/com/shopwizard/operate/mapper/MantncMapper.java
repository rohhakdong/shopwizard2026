package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.IdHolder;
import com.shopwizard.operate.model.Mantnc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 유지보수 요청(shopwizard.tOprMantnc). */
@Mapper
public interface MantncMapper {

    /** ownerId 가 있으면 그 사람이 올린 요청만. status: recipt·exec·cmplet·overdue. keyword/requester 는 LIKE 이스케이프를 마친 값. */
    List<Mantnc> selectList(@Param("ownerId") String ownerId, @Param("status") String status, @Param("keyword") String keyword,
                            @Param("requester") String requester, @Param("start") String start, @Param("end") String end,
                            @Param("offset") int offset, @Param("limit") int limit);

    int selectCount(@Param("ownerId") String ownerId, @Param("status") String status, @Param("keyword") String keyword,
                    @Param("requester") String requester, @Param("start") String start, @Param("end") String end);

    /** 상태별 건수: recipt, exec, cmplet, overdue. */
    Map<String, Object> selectSummary(@Param("ownerId") String ownerId);

    Mantnc select(@Param("mantncNo") Integer mantncNo);

    int insert(@Param("title") String title, @Param("content") String content, @Param("menuName") String menuName,
               @Param("roleUid") String roleUid, @Param("registId") String registId, @Param("registName") String registName,
               @Param("key") IdHolder key);

    /** 요청 내용 수정 — 작업완료 전만(0 이면 이미 완료). */
    int updateRequest(@Param("mantncNo") Integer mantncNo, @Param("title") String title, @Param("content") String content,
                      @Param("menuName") String menuName, @Param("changeId") String changeId, @Param("changeName") String changeName);

    /** 작업 예정일 입력·변경 — 작업완료 전만. 처음이면 작업중(exec)으로 바뀌고 접수자가 기록된다. */
    int updateSchedule(@Param("mantncNo") Integer mantncNo, @Param("expectDate") String expectDate, @Param("memo") String memo,
                       @Param("execId") String execId, @Param("execName") String execName);

    /** 작업완료 — 작업완료 전만(0 이면 이미 완료). */
    int updateComplete(@Param("mantncNo") Integer mantncNo, @Param("cntnts") String cntnts,
                       @Param("cmpletId") String cmpletId, @Param("cmpletName") String cmpletName);

    /** 삭제 — 작업완료 전만. */
    int delete(@Param("mantncNo") Integer mantncNo);
}
