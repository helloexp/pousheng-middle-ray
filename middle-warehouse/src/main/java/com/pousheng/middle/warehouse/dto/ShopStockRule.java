package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStockRule implements Serializable {

    private static final long serialVersionUID = -7574303583809832559L;

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺外码
     */
    private String outId;

    /**
     * 安全库存, 低于这个库存认为要推送的是0
     */
    private Long safeStock;

    /**
     * 库存分配比率, 以整数表示
     */
    private Integer ratio;

    /**
     * 虚拟库存，推送时加上，非必填
     */
    private Long jitStock;

    /**
     * 状态, 1 - 启用, -1 停用
     */
    private Integer status;

    /**
     * 上次推送数量
     */
    private Long lastPushStock;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}
