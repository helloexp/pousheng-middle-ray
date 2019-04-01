package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 同步淘宝的电商状态（目前只有官网，淘宝会用到该模块）
 */
public enum SyncTaobaoStatus {
    WAIT_SYNC_TAOBAO(0),//待同步淘宝
    SYNC_TAOBAO_SUCCESS(1),//发货单发货同步淘宝成功
    SYNC_TAOBAO_FAIL(-1);//发货单发货同步淘宝失败

    private final int value;

    SyncTaobaoStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SyncTaobaoStatus fromInt(int value){
        for (SyncTaobaoStatus orderStatus : SyncTaobaoStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown sync taobao status: "+value);
    }
}
