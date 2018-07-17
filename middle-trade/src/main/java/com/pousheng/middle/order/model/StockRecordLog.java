package com.pousheng.middle.order.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 下午12:06
 */
@Data
@ApiModel(value = "查询库存日志")
public class StockRecordLog implements Serializable {
    private static final long serialVersionUID = 9107628309656798854L;

    @ApiModelProperty(value = "ID", position = 1)
    private Long id;

    @ApiModelProperty(value = "发货单ID", position = 2)
    private Long shipmentId;

    @ApiModelProperty(value = "仓库ID", position = 3)
    private Long warehouseId;

    @ApiModelProperty(value = "店铺ID", position = 4)
    private Long shopId;

    @ApiModelProperty(value = "sku信息", position = 5)
    private String skuCode;

    @ApiModelProperty(value = "库存查询内容", position = 6)
    private String context;

    @ApiModelProperty(value = "日志类型", position = 7)
    private String type;

    @ApiModelProperty(value = "创建时间", position = 8)
    private Date createdAt;

    @ApiModelProperty(value = "更新时间", position = 9)
    private Date updatedAt;
}
