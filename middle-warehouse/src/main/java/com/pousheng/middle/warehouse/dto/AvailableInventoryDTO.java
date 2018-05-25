package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Author:  feisheng.ch
 * Date: 2018-05-28
 */
@Data
@AllArgsConstructor
public class AvailableInventoryDTO implements Serializable{

    private static final long serialVersionUID = 6253054158021294249L;
    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * 可用库存总数量
     */
    private Integer totalQuantity;

    /**
     * 渠道库存可用数量
     */
    private Integer channelQuantity;

    /**
     * 未分配渠道库存可用数量
     */
    private Integer inventoryLeftQuantity;

}
