package com.pousheng.middle.order.dto;

import com.pousheng.middle.order.model.PoushengGiftActivity;
import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@Data
public class PoushengGiftActivityInfo {
    private PoushengGiftActivity poushengGiftActivity;

    /**
     * 赠品
     */
    private List<GiftItem> giftItems;

    /**
     * 活动商品
     */
    private List<ActivityItem> activityItems;


    /**
     * 活动店铺
     */
    private List<ActivityShop> activityShops;
}
