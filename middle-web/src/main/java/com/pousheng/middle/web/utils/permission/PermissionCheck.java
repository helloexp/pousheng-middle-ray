package com.pousheng.middle.web.utils.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 检查当前登陆用户是否有操作该单据的权限
 * 单据：订单，子订单，逆向单，发货单
 * 依据：当前登陆用户可操作店铺列表
 * Created by sunbo@terminus.io on 2017/7/28.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PermissionCheck {

    /**
     * 权限检查类型
     *
     * @return
     */
    PermissionCheckType value();


    boolean throwExceptionWhenPermissionDeny() default false;

    enum PermissionCheckType {
        SHOP_ORDER, SHOP, REFUND,SHIPMENT
    }

}
