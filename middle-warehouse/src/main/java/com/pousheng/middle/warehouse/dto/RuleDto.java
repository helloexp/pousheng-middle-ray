package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;
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
     * 规则对应的区域
     */
    private List<ThinAddress>  addresses;
}
