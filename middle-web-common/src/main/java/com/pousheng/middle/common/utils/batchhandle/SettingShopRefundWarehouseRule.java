package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/9/3
 */
@Data
@ExportEditable(true)
public class SettingShopRefundWarehouseRule {
    @ExportTitle("销售店铺代码")
    private String orderShopCode;

    @ExportTitle("发货仓账套")
    private String shipmentCompanyId;

    @ExportTitle("退货仓代码")
    private String refundWarehouseCode;

    @ExportTitle("失败原因")
    private String failReason;
}
