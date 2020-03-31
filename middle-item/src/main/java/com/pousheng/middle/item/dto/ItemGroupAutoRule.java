package com.pousheng.middle.item.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
@Data
public class ItemGroupAutoRule implements Serializable {

    private static final long serialVersionUID = 2758143067511654406L;

    String name;

    Integer relation;

    String value;

    public Integer getRelation() {
        return relation;
    }

    public ItemGroupAutoRule relation(Integer relation) {
        this.relation = relation;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ItemGroupAutoRule value(String value) {
        this.value = value;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemGroupAutoRule name(String name) {
        this.name = name;
        return this;
    }
}
