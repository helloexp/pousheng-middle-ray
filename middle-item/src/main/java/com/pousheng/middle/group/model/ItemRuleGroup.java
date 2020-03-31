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
public class ItemRuleGroup implements Serializable {

    private Long id;

    /**

     * 商品规则id
     */
    private Long ruleId;

    /**
     * 分组id
     */
    private Long groupId;

    private Date createdAt;

    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public ItemRuleGroup id(Long id) {
        this.id = id;
        return this;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public ItemRuleGroup ruleId(Long ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public ItemRuleGroup groupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemRuleGroup createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemRuleGroup updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

}
