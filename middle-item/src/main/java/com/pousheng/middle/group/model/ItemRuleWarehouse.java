package com.pousheng.middle.group.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表Model类
 * Date: 2018-07-13
 */
@Data
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ItemRuleWarehouse implements Serializable {

    private Long id;

    /**
     * 商品规则id
     */
    private Long ruleId;

    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public ItemRuleWarehouse id(Long id) {
        this.id = id;
        return this;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public ItemRuleWarehouse ruleId(Long ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public ItemRuleWarehouse warehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemRuleWarehouse createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemRuleWarehouse updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }


}
