package com.pousheng.middle.warehouse.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Description: 店铺商品规则批量创建请求
 * User: support 9
 * Date: 2018/9/13
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "店铺商品规则批量创建请求")
public class ShopSkuSupplyRuleBatchCreateRequest implements Serializable {

    private static final long serialVersionUID = -6116907152917782153L;

    @ApiModelProperty(value = "店铺id", position = 1)
    private Long shopId;

    @ApiModelProperty(value = "店铺名称", position = 2)
    private String shopName;

    @ApiModelProperty(value = "货品条码", position = 3)
    private String skuCode;

    @ApiModelProperty(value = "货品名称", position = 4)
    private String skuName;

    @ApiModelProperty(value = "货号", position = 5)
    private String materialCode;

    @ApiModelProperty(value = "限制类型", position = 6)
    private String type;

    @ApiModelProperty(value = "规则状态", position = 7)
    private String status;

    @ApiModelProperty(value = "仓库列表", position = 8)
    private List<String> warehouses;

    @ApiModelProperty(value = "增量创建", position = 9)
    private Boolean delta;
}
