package com.pousheng.middle.web.warehouses.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/25
 */
@Data
public class StockPushLogDto implements Serializable {
    private static final long serialVersionUID = 3136434557890729574L;

    private Long id;
    private Long shopId;
    private String outId;
    private String shopName;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseOuterCode;

    private String skuCode;
    /**
     * 渠道skuId
     */
    private String channelSkuId;
    private String materialId;
    /**
     * 1.成功,2.失败
     */
    private int status;
    private String cause;
    /**
     * 目标库存
     */
    private String quantity;
    /**
     * 借用 实际调整库存 出入库用+/-
     */
    private String requestNo;

    private String lineNo;
    private Date syncAt;
    private Date createdAt;
    private Date updatedAt;
}
