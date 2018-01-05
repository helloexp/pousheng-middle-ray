/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.enums;

/**
 * @author : panxin
 */
public enum MemberFromType {

    // from是id
    // from / fromType (注册店铺id /注册店铺类型（ 1、店铺   2、运动城  3、店柜   4、异业渠道）
    SHOP(1),
    SPORT_CITY(2),
    SHOP_STORE(3);

    private final int value;

    public int value() {
        return value;
    }

    MemberFromType(int value) {
        this.value = value;
    }

}
