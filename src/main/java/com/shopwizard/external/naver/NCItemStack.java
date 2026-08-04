package com.shopwizard.external.naver;

import lombok.Data;

@Data
public class NCItemStack {
    private String itemId;
    private String itemName;
    private int itemTPrice;
    private int itemUPrice;
    private String selectedOption;
    private int count;
}
