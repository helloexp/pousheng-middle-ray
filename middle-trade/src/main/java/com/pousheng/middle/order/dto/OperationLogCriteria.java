package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by sunbo@terminus.io on 2017/8/2.
 */
@Data
public class OperationLogCriteria extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -1405649447349858395L;

    /**
     * 模块
     */
    private Integer type;

    /**
     * 操作人名
     */
    private String operatorName;


    /**
     * 操作的实体ID
     */
    private String operateId;

}
