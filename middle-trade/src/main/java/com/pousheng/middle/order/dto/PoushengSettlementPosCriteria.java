package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Data
public class PoushengSettlementPosCriteria extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -1405649447349858395L;

    /**
     * pos单类型
     */
    private Integer posType;
    /**
     * 发货单类型 1.销售发货单，2.换货发货单，3.售后单
     */
    private Integer shipType;
    /**
     * pos单号
     */
    private String posSerialNo;


    /**
     * 订单主键
     */
    private String orderId;

    /**
     * 店铺主键
     */
    private Long shopId;

    /**
     * 店铺主键集合
     */
    private List<Long> shopIds;

    /**
     * 开始时间
     */
    private Date startAt;

    /**
     * 结束时间
     */
    private Date endAt;
}
