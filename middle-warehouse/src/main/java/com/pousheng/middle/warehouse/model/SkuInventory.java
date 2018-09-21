package com.pousheng.middle.warehouse.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Author:  feisheng.ch
 * Date: 2018-05-18
 */
@Data
@ApiModel("库存列表中库存的模型")
@AllArgsConstructor
@NoArgsConstructor
public class SkuInventory implements Serializable {

    private static final long serialVersionUID = 1439907674352203884L;

    @ApiModelProperty(value = "库存ID主键", position = 1)
    private Long id;
    @ApiModelProperty(value = "货号", position = 2)
    private String materialCode;
    @ApiModelProperty(value = "货品条码", position = 3)
    private String skuCode;
    @ApiModelProperty(value = "仓库名称", position = 4)
    private String warehouseName;
    @ApiModelProperty(value = "仓库ID主键", position = 5)
    private Long warehouseId;
    @ApiModelProperty(value = "仓库编号", position = 6)
    private String warehouseCode;
    @ApiModelProperty(value = "基准库存", position = 7)
    private Long realQuantity;
    @ApiModelProperty(value = "可用库存", position = 8)
    private Long availableQuantity;
    @ApiModelProperty(value = "锁定库存", position = 9)
    private Long occupyQuantity;
    @ApiModelProperty(value = "商品安全库存", position = 10)
    private Long safeQuantitySku;
    @ApiModelProperty(value = "仓库安全库存", position = 11)
    private Long safeQuantityWarehouse;
    @ApiModelProperty(value = "指定库存次数", position = 12)
    private Integer channelCount;
    @ApiModelProperty(value = "尺码", position = 13)
    private String size;

}
