package com.pousheng.middle.search.stock;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-18 14:25<br/>
 */
@Data
@ApiModel("发货仓查询结果")
public class StockSendRuleDTO implements Serializable {
    private static final long serialVersionUID = 4779175932386299246L;

    @ApiModelProperty("搜索主键")
    private Object id;
    @ApiModelProperty("店铺 ID")
    private Long shopId;
    @ApiModelProperty("店铺类型")
    private Long shopType;
    @ApiModelProperty("规则ID")
    private Long ruleId;
    @ApiModelProperty("店铺名")
    private String shopName;
    @ApiModelProperty("店铺代码")
    private String shopOutCode;
    @ApiModelProperty("店铺账套")
    private String companyCode;

    @ApiModelProperty("发货区部 ID")
    private Long zoneId;
    @ApiModelProperty("发货区部")
    private String zoneName;

    @ApiModelProperty("仓库 ID")
    private Long warehouseId;
    @ApiModelProperty("仓库")
    private String warehouseName;
    @ApiModelProperty("仓库外码")
    private String warehouseOutCode;
    @ApiModelProperty("仓库账套")
    private String warehouseCompanyCode;
    @ApiModelProperty("仓库类型")
    private Integer warehouseType;
    @ApiModelProperty("仓库状态")
    private Integer warehouseStatus;
}
