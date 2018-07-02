package com.pousheng.middle.order.dispatch.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 门店或仓的距离权重
 * 仓的话一定要排在门店的前面
 * Created by songrenfei on 2017/12/26
 */
@Data
public class DispatchWithPriority implements Serializable{

    private static final long serialVersionUID = 316107445277208163L;

    private String warehouseOrShopId;

    private Double distance;

    /**
     * 优先级
     */
    private Integer priority;
}
