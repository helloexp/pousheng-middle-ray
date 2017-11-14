package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import lombok.Data;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-09-08
 */
@Data
public class WarehouseRuleItemDto extends WarehouseRuleItem{
    private static final long serialVersionUID = 8277164729139155788L;

    private String companyCode;

    /**
     * 仓库外码
     */
    private String outCode;
}
