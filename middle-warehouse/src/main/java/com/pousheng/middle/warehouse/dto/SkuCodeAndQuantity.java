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

    /**
     * 发货数量
     */
    private Integer shipQuantity;

    /**
     * 发货明细Id
     */
    private Long shipmentItemId;

    public Integer getShipQuantity() {
        return shipQuantity;
    }

    public SkuCodeAndQuantity shipQuantity(Integer shipQuantity) {
        this.shipQuantity = shipQuantity;
        return this;
    }

    public Long getSkuOrderId() {
        return skuOrderId;
    }

    public SkuCodeAndQuantity skuOrderId(Long skuOrderId) {
        this.skuOrderId = skuOrderId;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public SkuCodeAndQuantity skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public SkuCodeAndQuantity quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
