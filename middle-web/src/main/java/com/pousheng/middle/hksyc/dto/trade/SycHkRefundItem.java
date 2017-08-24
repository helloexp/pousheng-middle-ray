package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefundItem implements Serializable {


    private static final long serialVersionUID = 7916600025743499390L;

    private String refundSubNo;
    private String orderSubNo;
    private String barCode;
    private Integer itemNum;
    private String reason;
    private Integer salePrice;
    private Integer refundAmount;
    private String itemName;
}
