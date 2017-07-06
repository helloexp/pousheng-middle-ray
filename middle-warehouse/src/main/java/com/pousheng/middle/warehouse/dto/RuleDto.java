package com.pousheng.middle.warehouse.dto;

import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 规则概述
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Data
public class RuleDto implements Serializable {

    private static final long serialVersionUID = -557448986173832968L;

    /**
     * 规则id
     */
    private Long ruleId;

    /**
     * 规则描述
     */
    private String ruleDesc;

    /**
     * 规则关联的店铺
     */
    private List<ThinShop> shops;

    /**
     * 规则对应的区域
     */
    private List<ThinAddress>  addresses;

    /**
     * 规则对应的发货仓库
     */
    private List<WarehouseRuleItem> ruleItems;

    /**
     * 最后更新时间
     */
    private Date updatedAt;
}
