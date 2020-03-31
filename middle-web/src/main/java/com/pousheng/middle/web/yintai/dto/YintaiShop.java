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
@ApiModel("银泰店铺model")
@Data
public class YintaiShop extends BaseRowModel implements Serializable {
    private static final long serialVersionUID = -1978185039562080592L;

    @ApiModelProperty("店铺id")
    private Long id;
    @ExcelProperty(index = 0)
    @ApiModelProperty("公司码")
    private String companyCode;
    @ExcelProperty(index = 1)
    @ApiModelProperty("店铺外码")
    private String shopOutCode;
    @ApiModelProperty("店铺名称")
    private String shopName;
    @ExcelProperty(index = 2)
    @ApiModelProperty("银泰专柜ID")
    private String channelShopId;
}
