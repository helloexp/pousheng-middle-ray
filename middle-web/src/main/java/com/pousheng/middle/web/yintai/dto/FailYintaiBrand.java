package com.pousheng.middle.web.yintai.dto;

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
public class FailYintaiBrand implements Serializable {
    private static final long serialVersionUID = -6526629899514529259L;

    @ExportTitle("中台品牌ID")
    @ApiModelProperty("品牌ID")
    private String brandId;
    @ExportTitle("中台品牌名称")
    @ApiModelProperty("品牌名称")
    private String brandName;
    @ExportTitle("银泰品牌ID")
    @ApiModelProperty("银泰品牌ID")
    private String channelBrandId;
    @ExportTitle("导入失败原因")
    private String faildReason;
}
