package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 * @author tony
 */
@Data
public class PoushengSettlementPos implements Serializable {
    private static final long serialVersionUID = -1694328555026187228L;

    private Long id;
    /**
     * pos单类型:1.正常销售,2.售后
     */
    private Integer posType;

    /**
     * 订单或者售后单状态
     */
    private Integer status;

    /**
     * 发货单id
     */
    private Long orderId;

    /**
     * pos单号
     */
    private String posSerialNo;

    /**
     * pos单金额
     */
    private Long posAmt;

    /**
     * 店铺主键
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * pos单创建时间
     */
    private Date posCreatedAt;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}
