package com.pousheng.middle.web.warehouses.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 *
 * AUTHOR: zhangbin
 * ON: 2019/8/13
 */
@Data
@ApiModel("售后换货选择可发货仓request")
public class AfterSaleWarehouseRequest extends CommonChooseWarehouse implements Serializable {
    private static final long serialVersionUID = -3669183554728225134L;

    @ApiModelProperty(value = "交易单号", required = true)
    private String orderCode;
    @ApiModelProperty(value = "货品条码", required = true)
    private String skuCode;
}
