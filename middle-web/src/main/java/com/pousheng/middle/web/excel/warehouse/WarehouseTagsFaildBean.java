package com.pousheng.middle.web.excel.warehouse;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/6/6
 */
@Data
@ExportEditable(true)
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseTagsFaildBean {

    @ExportTitle("仓库外码")
    private String outWarehouseCode;

    @ExportTitle("标签2")
    private String tag2;

    @ExportTitle("标签3")
    private String tag3;

    @ExportTitle("错误信息")
    private String errorMsg;
}