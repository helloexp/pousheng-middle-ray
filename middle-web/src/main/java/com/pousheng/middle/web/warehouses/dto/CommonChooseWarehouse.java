package com.pousheng.middle.web.warehouses.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/8/13
 */
@Data
public class CommonChooseWarehouse implements Serializable {
    private static final long serialVersionUID = -7299992693780044178L;

    @ApiModelProperty(value = "公司码")
    private String companyCode;
    @ApiModelProperty(value = "外码")
    private String outCode;
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "是否排除 全部:0; 是:1; 否:-1")
    private Integer exclude;
    @ApiModelProperty(value = "是否发货限制 全部:0; 是:1; 否:-1")
    private Integer dispatchLimit;
    @ApiModelProperty(value = "是否可派发 全部:0; 是:1; 否:-1")
    private Integer available;

    private Integer pageNo;

    private Integer pageSize;
}
