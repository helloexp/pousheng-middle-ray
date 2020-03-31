package com.pousheng.middle.web.warehouses.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 选择可选发货仓
 * AUTHOR: zhangbin
 * ON: 2019/8/9
 */
@ApiModel("可发货仓")
@Data
public class SendWarehouseDTO implements Serializable {
    private static final long serialVersionUID = 1324554129437267741L;
    @ApiModelProperty(value = "仓库id")
    private Long warehouseId;
    @ApiModelProperty(value = "公司码")
    private String companyCode;
    @ApiModelProperty(value = "外码")
    private String outCode;//总仓&店仓
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "当前门店是否拒单")
    private Boolean hasReject = false;
    @ApiModelProperty(value = "是否排除")
    private Boolean isExclude = false;
    @ApiModelProperty(value = "是否发货限制外")
    private Boolean isDispatchLimit = false;
    @ApiModelProperty(value = "可用库存")
    private Integer quantity;
    @ApiModelProperty(value = "安全库存")
    private Integer safeQuantity;
    @ApiModelProperty(value = "是否可派发")
    private Boolean isAvailable = true;

    private String warehouseCode;
    //仓库类型 1店仓，0总仓
    private Integer warehouseSubType = 1;
    //状态
    private Integer status;

    private String skuCode;
}
