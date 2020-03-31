package com.pousheng.middle.group.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Data
public class ItemGroupCriteria extends PagingCriteria  {

    private String name;
    
    private List<Long> ids;
}
