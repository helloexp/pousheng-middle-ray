package com.pousheng.middle.web.events.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * mpos订单状态更新事件
 * Created by penghui on 2017/12/22
 */
@Data
public class MposOrderUpdateEvent implements Serializable{

    private static final long serialVersionUID = -3222888769931865929L;

    /**
     * 订单号
     */
    private Long orderId;

    /**
     * 更新类型
     */
    private String type;


    public MposOrderUpdateEvent(Long orderId,String type){
        this.orderId = orderId;
        this.type = type;
    }

}
