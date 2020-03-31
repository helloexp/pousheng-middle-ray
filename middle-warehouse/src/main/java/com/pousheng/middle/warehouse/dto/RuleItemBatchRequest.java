package com.pousheng.middle.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-24 10:11<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleItemBatchRequest implements Serializable {
    private static final long serialVersionUID = 2920573106732901015L;
    private Boolean isDelete;
    private Boolean isAll;
    private List<Long> shopIds;
    private List<Long> warehouseIds;
}

