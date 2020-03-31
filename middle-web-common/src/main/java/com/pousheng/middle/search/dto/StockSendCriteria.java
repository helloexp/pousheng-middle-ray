package com.pousheng.middle.search.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.terminus.parana.common.model.Criteria;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-18 14:17<br/>
 */
@ApiModel("发货仓查询条件")
@Data
public class StockSendCriteria extends Criteria implements Serializable {
    private static final long serialVersionUID = -3474091669257650958L;

    @ApiModelProperty("店铺ID")
    private Long shopId;
    @ApiModelProperty("店铺类型")
    private Long shopType;
    @ApiModelProperty("店铺ID列表")
    private List<Long> shopIds;
    @ApiModelProperty("店铺名")
    private String shopName;
    @ApiModelProperty("账套")
    private String companyCode;
    @ApiModelProperty("店铺外码")
    private String shopOutCode;

    @ApiModelProperty("区部ID")
    private Long zoneId;
    @ApiModelProperty("区部ID列表")
    private List<Long> zoneIds;

    @ApiModelProperty("仓库 ID")
    private Long warehouseId;
    @ApiModelProperty("仓库名称")
    private String warehouseName;
    @ApiModelProperty("发货仓帐套")
    private String warehouseCompanyCode;
    @ApiModelProperty("仓库外码")
    private String warehouseOutCode;

    private Integer pageNo;
    private Integer pageSize;
}
