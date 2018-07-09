package com.pousheng.middle.category.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

@Data
public class CategoryCriteria extends PagingCriteria {

    private Integer level;

    private String name;
}
