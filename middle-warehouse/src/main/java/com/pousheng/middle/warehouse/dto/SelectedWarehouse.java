package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 根据发货地址及sku数量返回的仓库结果
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Data
public class SelectedWarehouse implements Serializable {
    private static final long serialVersionUID = 7789080385812913946L;

    private Long warehouseId;

    private String warehouseName;

    private List<SkuCodeAndQuantity> skuCodeAndQuantities;
}
