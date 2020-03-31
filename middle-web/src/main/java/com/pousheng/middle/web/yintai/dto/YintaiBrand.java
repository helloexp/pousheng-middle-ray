package com.pousheng.middle.web.yintai.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/25
 */
@ApiModel("银泰品牌model")
@Data
public class YintaiBrand extends BaseRowModel implements Serializable {
    private static final long serialVersionUID = -8481688138799916474L;

    @ExcelProperty(value = "中台品牌ID", index = 0)
    @ApiModelProperty("品牌ID")
    private String brandId;
    @ExcelProperty(value = "中台品牌名称", index = 1)
    @ApiModelProperty("品牌名称")
    private String brandName;
    @ExcelProperty(value = "银泰品牌ID", index = 2)
    @ApiModelProperty("银泰品牌ID")
    private String channelBrandId;
}
