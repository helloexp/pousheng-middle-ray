package com.pousheng.middle.web.item.batchhandle;

import com.pousheng.middle.web.utils.export.ExportEditable;
import com.pousheng.middle.web.utils.export.ExportTitle;
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

    //条码
    @ExportTitle("条码")
    private String skuCode;

    //原因
    @ExportTitle("异常原因")
    private String reason;

}
