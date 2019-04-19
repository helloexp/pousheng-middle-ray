package com.pousheng.middle.web.excel.supplyRule.dto;

import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:14<br/>
 */
@Data
public class SupplyRuleDTO implements Serializable {
    private static final long serialVersionUID = 5844249308191288432L;

    private OpenClientShop shop;
    private List<String> warehouseCodes;
    private List<SkuTemplate> skuTemplate;
    private String status;
    private String type;

    private Integer rowNo;
    private Long total;
    private String failReason;
    private List<String> source;
}
