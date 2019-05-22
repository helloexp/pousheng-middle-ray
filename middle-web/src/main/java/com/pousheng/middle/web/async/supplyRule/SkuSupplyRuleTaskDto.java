package com.pousheng.middle.web.async.supplyRule;

import io.terminus.common.model.BaseUser;
import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/20
 */
@Data
public class SkuSupplyRuleTaskDto implements Serializable {
    private static final long serialVersionUID = -6127829105652492762L;

    private Long taskId;

    private Long shopId;

    private Long brandId;

    private Long operatorId;

    private String operatorName;
}
