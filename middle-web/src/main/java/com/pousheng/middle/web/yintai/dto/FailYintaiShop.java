package com.pousheng.middle.web.yintai.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/26
 */
@AllArgsConstructor
@NoArgsConstructor
@ExportEditable(true)
@Data
public class FailYintaiShop implements Serializable {
    private static final long serialVersionUID = 5696588825477594641L;

    @ExportTitle("公司码")
    private String companyCode;
    @ExportTitle("门店外码")
    private String shopOutCode;
    @ExportTitle("银泰专柜ID")
    private String channelShopId;
    @ExportTitle("导入失败原因")
    private String faildReason;
}
