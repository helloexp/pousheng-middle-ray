package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

/**
 * 异常记录
 */
@Data
@ExportEditable(true)
public class AbnormalPriorityItemRecord {

    @ExportTitle("公司编码")
    private String companyCode;

    @ExportTitle("仓库外码")
    private String watrehouseCode;

    @ExportTitle("优先级")
    private String priority;

    //原因
    @ExportTitle("异常原因")
    private String reason;

}
