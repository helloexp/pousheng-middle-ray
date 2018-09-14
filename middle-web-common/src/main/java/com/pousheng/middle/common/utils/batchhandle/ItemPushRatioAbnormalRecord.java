package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

/**
 * Description: 商品推送比例异常记录
 * User: support 9
 * Date: 2018/9/3
 */
@Data
@ExportEditable(true)
public class ItemPushRatioAbnormalRecord {

    @ExportTitle("外部电商平台id")
    private String openShopId;

    @ExportTitle("item")
    private String channelItemId;

    @ExportTitle("sku")
    private String channelSkuId;

    @ExportTitle("商品推送比例")
    private String ratio;

    @ExportTitle("失败原因")
    private String failReason;
}
