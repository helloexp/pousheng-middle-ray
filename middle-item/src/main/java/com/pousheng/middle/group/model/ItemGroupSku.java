package com.pousheng.middle.group.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Data
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ItemGroupSku {

    private Long id;

    private Long groupId;

    private String skuCode;

    private Date createdAt;

    private Date updatedAt;

    private Integer type;

    private Integer mark;

    public Integer getType() {
        return type;
    }

    public ItemGroupSku type(Integer type) {
        this.type = type;
        return this;
    }

    public Long getId() {
        return id;
    }

    public ItemGroupSku id(Long id) {
        this.id = id;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public ItemGroupSku groupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public ItemGroupSku skuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemGroupSku createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemGroupSku updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getMark() {
        return mark;
    }

    public ItemGroupSku mark(Integer mark) {
        this.mark = mark;
        return this;
    }
}
