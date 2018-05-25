package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 交易过程中对库存的操作
 * Author:  feisheng.ch
 * Date: 2018-05-24
 */
@Data
@AllArgsConstructor
@Builder
public class InventoryTradeDTO implements Serializable {

    /**
     * 下单店铺ID
     */
    private Long shopId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 子订单ID，如果没有，则使用订单ID
     */
    private List<String> subOrderId;

    /**
     * 特殊情况下的内容区分不同交易请求的标识，到库存中心后会进行二次处理
     */
    private String uniqueCode;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * skuCode
     */
    private String skuCode;

    /**
     * 交易数量
     */
    private Integer quantity;

    public InventoryTradeDTO () {}

}
