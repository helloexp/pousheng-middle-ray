package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/9/12
 */
@Data
public class ShopStockRuleDto implements Serializable {

    private static final long serialVersionUID = -5184533619253829691L;

    private Map<Long, ShopStockRule> warehouseRule;

    private ShopStockRule shopRule;
}
