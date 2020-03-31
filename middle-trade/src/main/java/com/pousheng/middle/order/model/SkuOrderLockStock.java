package com.pousheng.middle.order.model;

import lombok.Data;

@Data
public class SkuOrderLockStock {

    private String skuCode;

    private Long shopId;

    private Long warehouseId;

    private Integer quantity;
}
