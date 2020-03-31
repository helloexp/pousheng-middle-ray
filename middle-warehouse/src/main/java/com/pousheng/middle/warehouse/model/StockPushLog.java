package com.pousheng.middle.warehouse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockPushLog implements Serializable {

    private static final long serialVersionUID = 7443563667855929351L;
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
    private Long quantity;
    /**
     * 借用 实际调整库存 出入库用+/-
     */
    private String requestNo;

    private String lineNo;
    private Date syncAt;
    private Date createdAt;
    private Date updatedAt;


}
