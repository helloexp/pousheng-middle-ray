package com.pousheng.middle.order.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@Data
public class EditSubmitGiftActivityInfo {
    /**
     * 活动商品
     */
    private List<ActivityItem> activityItems;
    /**
     * 赠品
     */
    private List<GiftItem> giftItems;
    /**
     * 活动店铺
     */
    private List<ActivityShop> activityShops;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动开始时间
     */
    private Date activityStartDate;

    /**
     * 活动结束时间
     */
    private Date activityEndDate;


    /**
     * 类型:1.金额满足  2.数量满足
     */
    private Integer activityType;

    /**
     * 满足活动的商品金额
     */
    private Integer fee;
    /**
     * 满足活动的商品数量
     */
    private Integer quantity;

    /**
     * 是否限制活动商品：不限制(true),限制(false)
     */
    private Boolean isNoLimitItem;

    /**
     * 限制参与人数:0不限制,大于0限制参与人数
     */
    private Integer limitQuantity;
}
