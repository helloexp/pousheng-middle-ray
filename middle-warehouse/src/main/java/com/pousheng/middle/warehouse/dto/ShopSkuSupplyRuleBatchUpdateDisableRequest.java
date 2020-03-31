package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopSkuSupplyRuleBatchUpdateDisableRequest implements Serializable {
    private static final long serialVersionUID = 7626596892735514420L;

    private Long shopId;

    private List<String> skuCodes;

    private Long upperLimitId;
}
