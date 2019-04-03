package com.pousheng.middle.task.enums;


/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum TaskTypeEnum {

    ITEM_GROUP(1, "商品分组"),

    ITEM_GROUP_IMPORT(2, "商品分组导入"),

    SUPPLY_RULE_IMPORT(3, "发货限制导入");

    private final Integer value;
    private final String desc;

    TaskTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }


}
