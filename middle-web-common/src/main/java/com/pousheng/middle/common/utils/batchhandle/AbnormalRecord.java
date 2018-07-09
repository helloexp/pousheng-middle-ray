package com.pousheng.middle.common.utils.batchhandle;

import lombok.Data;

/**
 * 异常记录
 */
@Data
@ExportEditable(true)
public class AbnormalRecord {

    //编号
    @ExportTitle("货号")
    private String code;


    //尺码
    @ExportTitle("尺码")
    private String size;


    //名称
    @ExportTitle("名称")
    private String name;

    //条码
    @ExportTitle("条码")
    private String skuCode;

    //原因
    @ExportTitle("异常原因")
    private String reason;

}
