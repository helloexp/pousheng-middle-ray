package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 仓库发货明细
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Data
public class WarehouseShipment implements Serializable {
    private static final long serialVersionUID = 7789080385812913946L;

    private Long warehouseId;

    private String warehouseName;

    private List<SkuCodeAndQuantity> skuCodeAndQuantities;
}
