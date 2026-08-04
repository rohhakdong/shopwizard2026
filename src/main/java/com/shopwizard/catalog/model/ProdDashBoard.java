package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class ProdDashBoard {
    private String shopCode;
    private String shopName;
    private Integer saleYesCount;
    private Integer saleNoCount;
    private Integer saleReadyCount;
    private Integer saleDeleteCount;
}
