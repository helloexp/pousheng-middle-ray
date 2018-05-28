package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import lombok.Data;
import lombok.Setter;

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

    /**
     * 仓库类别 0 总仓 1 店仓
     */
    @Setter
    private Integer type;

    /**
     * 仓库状态
     */
    @Setter
    private Integer status;

    /**
     * 仓库地址
     */
    @Setter
    private String address;

}
