package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 待发货商品信息封装 for 销售发货 和 售后发货
 * Created by songrenfei on 2017/7/1
 */
@Data
public class WaitShipItemInfo extends BasicItemInfo implements Serializable{


    private static final long serialVersionUID = -1992278174788110480L;

    //待处理数量即待发货数量
    private Integer waitHandleNumber;

    //子单号
    private Long skuOrderId;






}
