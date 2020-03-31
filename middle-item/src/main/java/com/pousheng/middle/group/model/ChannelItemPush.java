package com.pousheng.middle.group.model;

import lombok.Data;

import java.util.Date;

/**
 * 渠道商品推送状态
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
@Data
public class ChannelItemPush {

    private Long id;

    private Long brandId;

    private String brandName;

    private String spuCode;

    private String spuName;

    private Long skuId;

    private String skuCode;

    private String channel;

    private Long openShopId;

    private String openShopName;

    private String channelItemId;

    private String channelSkuId;

    private String channelBrandId;
    /**
     * @see com.pousheng.middle.item.constant.ItemPushStatus
     */
    private Integer status;

    private String color;

    private String size;

    private Integer price;

    private Date createdAt;

    private Date updatedAt;
}
