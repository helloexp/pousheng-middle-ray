package com.pousheng.middle.task.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Data
public class TaskSearchCriteria extends PagingCriteria{

    private Integer type;

    private Long businessId;

    private Integer businessType;

    private Long userId;
}
