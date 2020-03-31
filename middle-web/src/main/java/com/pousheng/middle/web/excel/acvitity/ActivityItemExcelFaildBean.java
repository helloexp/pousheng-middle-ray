package com.pousheng.middle.web.excel.acvitity;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/6/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ExportEditable(true)
public class ActivityItemExcelFaildBean {
    //货号
    @ExportTitle("货号")
    private String itemCode;

    //错误信息
    @ExportTitle("失败原因")
    private String msg;
}
