package com.pousheng.middle.group.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Data
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ItemRule implements Serializable {

    private Long id;

    /**
     * 规则名称
     */
    private String name;

    private Integer type;

    private Date createdAt;

    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public Integer getType() {
        return type;
    }

    public ItemRule type(Integer type) {
        this.type = type;
        return this;
    }

    public ItemRule id(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemRule name(String name) {
        this.name = name;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ItemRule createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ItemRule updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
