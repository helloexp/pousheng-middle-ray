package com.pousheng.middle.group.dto;


import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

@Data
public class ItemGroupSkuCriteria extends PagingCriteria {
    
    private Long groupId;
    
    private Integer type;
}
