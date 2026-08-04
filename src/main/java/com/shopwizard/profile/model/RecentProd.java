package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class RecentProd {
    private String sessonId;
    private String prodCode;
    private Integer custId;
    private String prodImgName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
