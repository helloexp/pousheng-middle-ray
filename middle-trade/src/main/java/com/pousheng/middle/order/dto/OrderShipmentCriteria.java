package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/16
 */
@Data
public class OrderShipmentCriteria extends PagingCriteria implements Serializable{


    private static final long serialVersionUID = 4275154324867522562L;
    /**
     * 发货单id
     */
    private String shipmentId;

    /**
     * 订单类型
     * @see io.terminus.parana.order.enums.ShipmentType
     */
    private Integer type;
    /**
     * 创建开始时间
     */
    private Date startAt;

    /**
     * 创建结束时间
     */
    private Date endAt;


    /**
     * 订单id
     */
    private Long orderId;


    /**
     * 店铺ids，用于控制用户可操作的店铺
     */
    private List<Long> shopIds;

    /**
     * 售后单id
     */
    private Long afterSaleOrderId;


    /**
     * 状态
     */
    private Integer status;

    /**
     * 订单类型
     * @see io.terminus.parana.order.model.OrderLevel
     */
    private Integer orderType;

    /**
     * 订单来源
     */
    private Long shopId;



    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    @Override
    public void formatDate(){
        if(startAt != null && endAt != null){
            if(startAt.equals(endAt)){
                endAt=new DateTime(endAt.getTime()).plusDays(1).minusSeconds(1).toDate();
            }
        }
    }

}
