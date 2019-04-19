package com.pousheng.middle.warehouse.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-15 17:05<br/>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@ApiModel(description = "店铺商品供货规则分页请求")
public class ShopSkuSupplyRuleQueryOneRequest implements Serializable {

    private static final long serialVersionUID = 8354766758458637829L;

    @ApiModelProperty(value = "店铺名称", position = 1)
    private Long shopId;

    @ApiModelProperty(value = "货品条码", position = 2)
    private String skuCode;

    @ApiModelProperty(value = "货号", position = 3)
    private String materialCode;

    @ApiModelProperty(value = "限制类型", position = 4)
    private String type;

    @ApiModelProperty(value = "状态", position = 5)
    private String status;
}
