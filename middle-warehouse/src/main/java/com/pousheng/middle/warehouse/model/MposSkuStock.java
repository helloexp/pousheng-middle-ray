package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * mpos 商品占用库存
 * Created by songrenfei on 2017/12/23
 */
@Data
public class MposSkuStock implements Serializable{

    private static final long serialVersionUID = -3532008580936759580L;

    private Long id;

    /**
     * 仓ID
     */
    private Long warehouseId;

    /**
     * 门店Id
     */
    private Long shopId;

    /**
     * 商品编号
     */
    private String skuCode;

    /**
     * 当前锁定库存
     */
    private Long lockedStock;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
