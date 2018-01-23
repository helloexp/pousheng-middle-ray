package com.pousheng.middle.web.item.batchhandle;

import com.pousheng.middle.web.utils.export.ExportTitle;
import lombok.Data;

/**
 * 异常记录
 */
@Data
public class AbnormalRecord {

    //编号
    @ExportTitle("货号")
    private String code;

    //原因
    @ExportTitle("异常原因")
    private String reason;

}
