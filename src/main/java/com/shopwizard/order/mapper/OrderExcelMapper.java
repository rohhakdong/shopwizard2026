package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderExcelUpload;
import com.shopwizard.order.model.OrderExcelUploadDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 주문 엑셀 업로드 — 채널 조회, 원본 보관(shopion.tOrdOrderExcel, 중복 판정 기준), 업로드 이력. */
@Mapper
public interface OrderExcelMapper {

    /** 이름이 정확히 같은 채널 코드들(서비스가 주어지면 그 서비스 안에서만). */
    List<String> selectChnlCodesByName(@Param("chnlName") String chnlName, @Param("svcCode") String svcCode);

    /** 이미 처리된(원본이 보관된) 엑셀 주문번호들. */
    List<String> selectExistingExcelOrderNos(@Param("nos") Collection<String> nos);

    /** 원본 보관 한 건 — 키는 tOrdOrderExcel 컬럼 이름(첫 글자 소문자). 엑셀 주문번호가 이미 있으면 DuplicateKeyException. */
    int insertExcelOrder(Map<String, Object> row);

    int insertUpload(OrderExcelUpload upload);

    int finishUpload(@Param("uploadId") int uploadId, @Param("resultType") String resultType,
                     @Param("resultMessag") String resultMessag, @Param("changeId") String changeId,
                     @Param("changeName") String changeName);

    int insertUploadDetail(OrderExcelUploadDetail detail);

    List<OrderExcelUpload> selectUploadList(@Param("startDate") String startDate, @Param("endDate") String endDate,
                                            @Param("registId") String registId,
                                            @Param("offset") int offset, @Param("size") int size);

    int selectUploadCount(@Param("startDate") String startDate, @Param("endDate") String endDate,
                          @Param("registId") String registId);

    OrderExcelUpload selectUpload(@Param("uploadId") int uploadId);

    List<OrderExcelUploadDetail> selectUploadDetails(@Param("uploadId") int uploadId);
}
