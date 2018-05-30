package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
@Data
public class StockPushLog {
    private Long id;
    private Long shopId;
    private String shopName;
    private String skuCode;
    private int status;//1.成功,2.失败
    private String cause;
    private Long quantity;
    private Date syncAt;
    private Date createdAt;
    private Date updatedAt;
}
