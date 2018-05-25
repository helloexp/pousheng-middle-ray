package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * skuCode和购买数量
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Data
public class SkuCodeAndQuantity implements Serializable {

    private static final long serialVersionUID = -7014447872532799068L;

    /**
     * 子订单ID
     */
    private Long skuOrderId;

    /**
     * sku code
     */
    private String  skuCode;

    /**
     * 购买数量
     */
    private Integer quantity;
}
