package com.pousheng.middle.web.utils.operationlog;

import lombok.Getter;
import org.apache.commons.collections.map.HashedMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by sunbo@terminus.io on 2017/8/1.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OperationLogModule {


    Module value();

    enum Module {

        UNKNOWN(0, "未知", "unknown"),

        ORDER(1, "交易订单", "order"),

        REFUND(2, "逆向单", "refund"),

        SHIPMENT(3, "发货单", "shipment"),

        WAREHOUSE(4, "仓库", "warehouse"),

        WAREHOUSE_RULE(5, "仓库规则", "warehouse/rule"),

        WAREHOUSE_COMPANY_RULE(6, "仓库公司规则", "warehouse/company-rule"),

        WAREHOUSE_RULE_ITEM(7, "仓库与规则关系", "warehouse/company-rule"),

        WAREHOUSE_ADDRESS_RULE(8, "仓库地址规则", "warehouse/address-rule"),

        WAREHOUSE_SHOP_RULE(9, "店铺发货规则", "warehouse/shop-rule");

        @Getter
        private int value;
        @Getter
        private String desc;
        @Getter
        private String key;


        Module(int value, String desc, String key) {
            this.value = value;
            this.desc = desc;
            this.key = key;
        }


        public static Module fromValue(int value) {
            Optional<Module> module = Stream.of(Module.values()).parallel().filter(m -> m.getValue() == value).findAny();
            if (module.isPresent())
                return module.get();

            return UNKNOWN;
        }

        public static Module fromKey(String key) {

            Optional<Module> module = Stream.of(Module.values()).parallel().filter(m -> m.getKey().equals(key)).findAny();
            if (module.isPresent())
                return module.get();

            return UNKNOWN;
        }

    }
}
