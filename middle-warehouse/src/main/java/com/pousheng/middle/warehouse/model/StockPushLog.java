package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Data
public class StockPushLog implements Serializable {

    private static final long serialVersionUID = 7443563667855929351L;
    private Long id;
    private Long shopId;
    private String outId;
    private String shopName;
    private String skuCode;
    private String materialId;
    /**
     * 1.成功,2.失败
     */
    private int status;
    private String cause;
    private Long quantity;
    private Date syncAt;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public StockPushLog id(Long id) {
        this.id = id;
        return this;
    }

    public Long getShopId() {
        return shopId;
    }

    public StockPushLog shopId(Long shopId) {
        this.shopId = shopId;
        return this;
    }

    public String getOutId() {
        return outId;
    }

    public StockPushLog outId(String outerId) {
        this.outId = outerId;
        return this;
    }

    public String getShopName() {
        return shopName;
    }

    public StockPushLog shopName(String shopName) {
        this.shopName = shopName;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public StockPushLog skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public String getMaterialId() {
        return materialId;
    }

    public StockPushLog materialId(String materialId) {
        this.materialId = materialId;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public StockPushLog status(int status) {
        this.status = status;
        return this;
    }

    public String getCause() {
        return cause;
    }

    public StockPushLog cause(String cause) {
        this.cause = cause;
        return this;
    }

    public Long getQuantity() {
        return quantity;
    }

    public StockPushLog quantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }

    public Date getSyncAt() {
        return syncAt;
    }

    public StockPushLog syncAt(Date syncAt) {
        this.syncAt = syncAt;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public StockPushLog createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public StockPushLog updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
