package com.pousheng.middle.item.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/8/15
 */
@Data
public class IndexedStockLog implements Serializable {


    private static final long serialVersionUID = -6856964950904293988L;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 发货单id
     */
    private Long shipmentId;

    /**
     * 商品编码
     */
    private String skuCode;


    /**
     * 货号
     */
    private String materialId;


    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 仓库code
     */
    private String warehouseCode;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 操作
     */
    private Integer operate;

    /**
     * 更新时间
     */
    private Date createdAt;


    public Integer getType() {
        return type;
    }

    public IndexedStockLog type(Integer type) {
        this.type = type;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public IndexedStockLog quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public IndexedStockLog shipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public IndexedStockLog skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public String getMaterialId() {
        return materialId;
    }

    public IndexedStockLog materialId(String materialId) {
        this.materialId = materialId;
        return this;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public IndexedStockLog warehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
        return this;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public IndexedStockLog warehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public IndexedStockLog status(Integer status) {
        this.status = status;
        return this;
    }

    public Integer getOperate() {
        return operate;
    }

    public IndexedStockLog operate(Integer operate) {
        this.operate = operate;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public IndexedStockLog createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
