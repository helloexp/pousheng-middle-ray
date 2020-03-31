package com.pousheng.middle.web.warehouses.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/8/12
 */
@ApiModel("手工派单选择可发货仓request")
@Data
public class SendWarehouseRequest extends CommonChooseWarehouse implements Serializable {
    private static final long serialVersionUID = -4977652801457135952L;

    @ApiModelProperty(value = "子单号(兼容)")
    private Long skuOrderId;
    @ApiModelProperty(value = "shopId(必填)")
    private Long shopId;
    @ApiModelProperty(value = "skuCode(必填)")
    private String skuCode;

}
