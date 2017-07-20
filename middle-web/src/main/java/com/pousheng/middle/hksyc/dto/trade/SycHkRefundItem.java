package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefundItem implements Serializable {


    private static final long serialVersionUID = 7916600025743499390L;

    private String refundSubNo = "001";
    private String orderSubNo = "001";
    private String barCode = "xh001";
    private Integer itemNum = 1;
    private String reason = "不想要了";
    private Integer salePrice = 199;
    private Integer refundAmount = 199;
    private String itemName = "鞋子-特卖";
}
