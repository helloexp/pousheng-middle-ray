package com.pousheng.middle.group.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: songrenfei
 * Desc: Model类
 * Date: 2018-05-07
 */
@Data
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ItemRuleShop implements Serializable {

    private Long id;

    /**
     * 商品规则id
     */
    private Long ruleId;

    /**
     * 店铺id
     */
    private Long shopId;

    private Date createdAt;

    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public ItemRuleShop id(Long id) {
        this.id = id;
        return this;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public ItemRuleShop ruleId(Long ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public Long getShopId() {
        return shopId;
    }

    public ItemRuleShop shopId(Long shopId) {
        this.shopId = shopId;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemRuleShop createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemRuleShop updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
