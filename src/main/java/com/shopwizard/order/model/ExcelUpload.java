package com.shopwizard.order.model;

import lombok.Data;

@Data
public class ExcelUpload {
    private Integer uploadId;
    private String uploadType;
    private String uploadFileName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String uploadStartDate;
    private String uploadEndDate;
    private String chnlCode;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
