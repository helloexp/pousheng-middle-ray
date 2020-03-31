package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

@Data
@ExportEditable(true)
public class SettingSafeStockRecord {

    @ExportTitle("账套")
    private String companyId;
    
    @ExportTitle("外码")
    private String outerId;
    
    @ExportTitle("货号")
    private String spuCode;
    
    @ExportTitle("安全库存")
    private String safeStock;
    
    /*@ExportTitle("告警邮箱")
    private String warningEmail;*/

    @ExportTitle("失败原因")
    private String failReason;
    
}
