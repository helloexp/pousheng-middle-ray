package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 同步渠道(云聚)的电商状态（
 */
public enum SyncChannelStatus {
    WAIT_SYNC_CHANNEL(0),//待同步渠道
    SYNC_CHANNE_SUCCESS(1),//发货单发货同步渠道成功
    SYNC_CHANNE_FAIL(-1);//发货单发货同步渠道失败

    private final int value;

    SyncChannelStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SyncChannelStatus fromInt(int value){
        for (SyncChannelStatus orderStatus : SyncChannelStatus.values()) {
            if(Objects.equal(orderStatus.value, value)){
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("unknown sync channel status: "+value);
    }
}
