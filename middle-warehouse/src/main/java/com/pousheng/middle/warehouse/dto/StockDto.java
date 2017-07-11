package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-11
 */
@Data
public class StockDto implements Serializable{
    private static final long serialVersionUID = 8350465916583376693L;

    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * 可用库存数量
     */
    private Integer quantity;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
