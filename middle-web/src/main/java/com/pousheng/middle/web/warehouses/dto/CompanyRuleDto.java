package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-08-28
 */
@Data
public class CompanyRuleDto extends WarehouseCompanyRule implements Serializable{
    private static final long serialVersionUID = 2383598985589538275L;

    /**
     * 仓库内码
     */
    private String warehouseCode;
}
