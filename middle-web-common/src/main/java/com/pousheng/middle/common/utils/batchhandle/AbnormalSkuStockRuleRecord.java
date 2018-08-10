package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

/**
 * 异常记录
 */
@Data
@ExportEditable(true)
public class AbnormalSkuStockRuleRecord {

    //货品条码
    @ExportTitle("货品条码")
    private String skuCode;

    //保障库存
    @ExportTitle("保障库存")
    private String safeStock;


    //推送比例
    @ExportTitle("推送比例")
    private String ratio;

    //虚拟库存
    @ExportTitle("虚拟库存")
    private String jitStock;

    //状态
    @ExportTitle("状态")
    private String status;

    //原因
    @ExportTitle("异常原因")
    private String reason;

}
