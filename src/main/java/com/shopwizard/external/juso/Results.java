package com.shopwizard.external.juso;

import lombok.Data;

import java.util.List;

@Data
public class Results {
    private int totalCount;
    private int currentPage;
    private int countPerPage;
    private String errorCode;
    private String errorMessage;
    private List<Juso> juso;
}
