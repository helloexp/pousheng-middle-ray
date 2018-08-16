package com.pousheng.middle.web.middleLog.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/8/17
 */
@Data
public class InventoryTradeLog implements Serializable {

    private static final long serialVersionUID = -6217132169794432165L;

    private Long shipmentId;

    private String warehouseCode;

    private String skuCode;

    private Long quantity;

    private Integer type;

    private Date createdAt;

    public Long getShipmentId() {
        return shipmentId;
    }

    public InventoryTradeLog shipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public InventoryTradeLog skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public InventoryTradeLog warehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
        return this;
    }

    public Long getQuantity() {
        return quantity;
    }

    public InventoryTradeLog quantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public InventoryTradeLog type(Integer type) {
        this.type = type;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public InventoryTradeLog createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
