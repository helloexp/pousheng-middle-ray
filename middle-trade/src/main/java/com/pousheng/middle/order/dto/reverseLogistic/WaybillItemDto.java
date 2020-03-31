package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 无头件商品信息
 * @author bernie
 * @date 2019/6/3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaybillItemDto implements Serializable {
    /**
     * 货号：通过goods货号
     */
    private String goodsNo;

    /**
     * sku信息
     */
    private String skuNo;
    /**
     * 尺码
     */
    private String size;

    /**
     * 品牌
     */
    private String brand;
    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 入库数量
     */
    private Integer instoreQuantity;
    /**
     * 左脚尺寸
     */
    private String leftSize;
    /**
     * 鞋盒信息
     */
    private String shoeboxInfo;
    /**
     * 实物信息
     */
    private String materialInfo;
    /**
     * 重量
     */
    private String weight;
    /**
     * 右脚尺寸
     */
    private String rightSize;
}
