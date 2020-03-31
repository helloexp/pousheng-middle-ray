package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/8
 */
@AllArgsConstructor
@NoArgsConstructor
@ExportEditable(true)
@Data
public class WarehouseImportFileErrorBean {

    @ExportTitle("外码")
    private String outCode;
    @ExportTitle("账套")
    private String companyCode;
    @ExportTitle("原因")
    private String msg;
}
