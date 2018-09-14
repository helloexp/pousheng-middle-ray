package com.pousheng.middle.common.utils.batchhandle;

import lombok.*;

/**
 * Description: 商品供货规则异常记录
 * User: support 9
 * Date: 2018/9/17
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ExportEditable(true)
public class ItemSupplyRuleAbnormalRecord {

    @ExportTitle("店铺")
    private String shop;

    @ExportTitle("货品条码")
    private String skuCode;

    @ExportTitle("限制类型")
    private String type;

    @ExportTitle("限制范围")
    private String warehouseCodes;

    @ExportTitle("状态")
    private String status;

    @ExportTitle("失败原因")
    private String failReason;
}
