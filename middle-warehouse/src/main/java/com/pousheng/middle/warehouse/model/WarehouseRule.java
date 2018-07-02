package com.pousheng.middle.warehouse.model;

import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import io.terminus.common.utils.Arguments;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述Model类
 * Date: 2017-06-07
 */
@Data
public class WarehouseRule implements Serializable {

    private static final long serialVersionUID = 2814989059726443700L;

    /**
     * 主键
     */
    private Long id;
    
    /**
     * 规则描述, 按照优先级将各仓名称拼接起来
     */
    private String name;

    /**
     * 店铺组id
     */
    private Long shopGroupId;

    /**
     * 仓库派单优先级类型
     */
    private Integer itemPriorityType;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 修改时间
     */
    private Date updatedAt;

    public Integer getItemPriorityType(){
        //如果没维护就默认按照优先级
        if(Arguments.isNull(itemPriorityType)){
            return WarehouseRuleItemPriorityType.DISTANCE.value();
        }
        return itemPriorityType;
    }

}
