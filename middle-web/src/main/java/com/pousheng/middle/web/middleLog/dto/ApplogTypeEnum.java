package com.pousheng.middle.web.middleLog.dto;

import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Objects;

/**
 * @author zhaoxw
 * @date 2018/8/20
 */
public enum ApplogTypeEnum {


    SET_SAFE_STOCK("库存设置安全库存数据", "SET_SAFE_STOCK"),

    SET_WAREHOUSR_SAFE_STOCK("仓库设置安全库存数据", "SET_WAREHOUSR_SAFE_STOCK"),

    SET_CHANNEL_STOCK("创建指定库存（渠道库存）", "SET_CHANNEL_STOCK"),

    BATCH_SET_CHANNEL_STOCK("批量创建指定库存（渠道库存）", "SET_CHANNEL_STOCK"),

    CREATE_WAREHOUSE_SKU_PUSH_RULE("创建商品级库存推送规则", "CREATE_WAREHOUSE_SKU_PUSH_RULE"),

    UPDATE_WAREHOUSE_SKU_PUSH_RULE("更新商品级库存推送规则", "UPDATE_WAREHOUSE_SKU_PUSH_RULE"),

    CREATE_WAREHOUSE_PUSH_RULE("创建仓库级库存推送规则", "CREATE_WAREHOUSE_PUSH_RULE"),

    UPDATE_WAREHOUSE_PUSH_RULE("更新仓库级库存推送规则", "UPDATE_WAREHOUSE_PUSH_RULE"),

    CREATE_SHOP_PUSH_RULE("创建店铺库存推送规则", "CREATE_SHOP_PUSH_RULE"),

    UPDATE_SHOP_PUSH_RULE("更新店铺库存推送规则", "UPDATE_SHOP_PUSH_RULE"),

    BATCH_WAREHOUSE_SKU_PUSH_RULE("批量编辑商品级推送规则", "BATCH_WAREHOUSE_SKU_PUSH_RULE"),

    BATCH_WAREHOUSE_PUSH_RULE("批量编辑仓库级库存推送规则", "BATCH_WAREHOUSE_USH_RULE");


    private final String value;

    private final String desc;

    ApplogTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值构造enum
     *
     * @param value key的值
     * @return
     */
    public static ApplogTypeEnum from(String value) {
        for (ApplogTypeEnum source : ApplogTypeEnum.values()) {
            if (Objects.equals(source.value, value)) {
                return source;
            }
        }
        return null;
    }

    public static List<String> getKeys() {
        List<String> list = Lists.newArrayList();
        for (ApplogTypeEnum source : ApplogTypeEnum.values()) {
            list.add(source.value);
        }
        return list;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }


}
