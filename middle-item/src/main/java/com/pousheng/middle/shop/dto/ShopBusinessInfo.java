package com.pousheng.middle.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Description: 门店营业信息DTO
 * User: liangyj
 * Date: 2018/5/8
 */
@Data
@ApiModel("门店营业信息")
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class ShopBusinessInfo implements Serializable {

    private static final long serialVersionUID = -1889152869171213266L;

    @ApiModelProperty(value = "门店ID")
    private Long id;

    @ApiModelProperty(value = "门店类型 1：综合门店；2：接单门店；3：下单门店")
    private Integer type;

    @ApiModelProperty(value = "商店外码ID")
    private String outId;

    @ApiModelProperty(value = "行业ID")
    private Long businessId;

    @ApiModelProperty(value = "门店经营时间，最大接单量")
    private ShopBusinessTime shopBusinessTime;

}
