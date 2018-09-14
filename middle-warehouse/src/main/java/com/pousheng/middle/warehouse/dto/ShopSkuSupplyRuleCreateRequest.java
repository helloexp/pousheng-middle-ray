package com.pousheng.middle.warehouse.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;

/**
 * Description: 店铺商品规则创建请求
 * User: support 9
 * Date: 2018/9/13
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "店铺商品规则创建请求")
public class ShopSkuSupplyRuleCreateRequest implements Serializable {

    private static final long serialVersionUID = 3150785229449146056L;

    @ApiModelProperty(value = "id", position = 1)
    private Long id;

    @ApiModelProperty(value = "店铺id", position = 2)
    private Long shopId;

    @ApiModelProperty(value = "店铺名称", position = 3)
    private String shopName;

    @ApiModelProperty(value = "货品条码", position = 4)
    private String skuCode;

    @ApiModelProperty(value = "货品名称", position = 5)
    private String skuName;

    @ApiModelProperty(value = "货号", position = 6)
    private String materialCode;

    @ApiModelProperty(value = "限制类型", position = 7)
    private String type;

}
