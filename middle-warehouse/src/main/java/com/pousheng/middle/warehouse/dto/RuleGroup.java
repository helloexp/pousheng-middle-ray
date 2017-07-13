package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 按照店铺组归组的发货规则列表
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-06
 */
@Data
public class RuleGroup implements Serializable {


    private static final long serialVersionUID = 4796694765986793331L;

    /**
     * 店铺组id
     */
    private Long shopGroupId;

    /**
     * 店铺组对应的店铺
     */
    private List<ThinShop> shops;

    /**
     * 归组的发货规则
     */
    private List<RuleSummary> ruleSummaries;
}
