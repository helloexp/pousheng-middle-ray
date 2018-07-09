package com.pousheng.middle.item.dto;

import lombok.Data;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */
@Data
public class ItemGroupAutoRule {

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
