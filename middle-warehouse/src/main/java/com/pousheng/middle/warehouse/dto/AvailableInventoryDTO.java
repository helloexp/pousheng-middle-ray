package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Author:  feisheng.ch
 * Date: 2018-05-28
 */
@Data
@AllArgsConstructor
public class AvailableInventoryDTO implements Serializable{

    private static final long serialVersionUID = 6253054158021294249L;
    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * 可用库存总数量
     */
    private Integer totalAvailQuantity;

    /**
     * 渠道库存自身的可用数量，减掉了占用等
     */
    private Integer channelAvailQuantity;

    /**
     * 渠道库存当前真实库存，对应DB中的RealQuantity
     */
    private Integer channelRealQuantity;

    /**
     * 所有分配出去的库存数
     */
    private Integer allChannelQuantity;

    /**
     * 真是库存数 = real-occupy-safe
     */
    private Integer realAvailableQuantity;

    /**
     * 未分配渠道库存可用数量 = real-occupy-safe-allAlloc+alloc
     */
    private Integer inventoryUnAllocQuantity;

    /**
     * 最终安全库存-商品和仓库比对后的
     */
    private Integer safeQuantity;

    /**
     * 去掉安全库存的真实可用库存数，包含了渠道数据
     *
     * @return 真实可用库存(去掉安全库存计算项)-所有指定库存+指定当前店铺的库存
     */
    public Integer getAvailableQuantityWithoutSafe () {
        return Math.min(Math.max(NonNull(realAvailableQuantity) + NonNull(safeQuantity), 0),
                Math.max(0,
                        NonNull(channelRealQuantity) +
                                NonNull(realAvailableQuantity) + NonNull(safeQuantity)
                                - NonNull(allChannelQuantity)
                )
        );
    }

    private Integer NonNull(Integer input) {
        if (null == input) {
            return 0;
        }

        return input;
    }

}
