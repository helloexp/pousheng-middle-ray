package com.pousheng.middle.task.enums;


/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum TaskTypeEnum {

    ITEM_GROUP(1, "商品分组"),

    ITEM_GROUP_IMPORT(2, "商品分组导入"),

    SUPPLY_RULE_IMPORT(3, "发货限制导入"),

    SUPPLY_RULE_BATCH_DISABLE(4, "发货规则批量禁用");

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
