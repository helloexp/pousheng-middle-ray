package com.pousheng.middle.web.async.supplyRule;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/14
 */
@Data
public class SkuSupplyRuleLog implements Serializable {
    private static final long serialVersionUID = 4192581469125111035L;

    private Long shopId;

    private String shopName;

    private Long brandId;

    private String brandName;

    private String operator;

    private Date createdAt;

    private Long count;
}
