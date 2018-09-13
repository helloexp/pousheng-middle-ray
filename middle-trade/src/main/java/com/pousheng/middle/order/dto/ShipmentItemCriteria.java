package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description: shipment item criteria
 * @author: yjc
 * @date: 2018/9/12下午5:26
 */
@Data
public class ShipmentItemCriteria extends PagingCriteria implements Serializable {
    private static final long serialVersionUID = -1882451608585657040L;

    /**
     * 发货单id
     */
    private Long shipmentId;

    /**
     * 发货单ids
     */
    private List<Long> shipmentIds;

    /**
     * 门店ids
     */
    private List<Long> shopIds;


    /**
     * 仓库ids
     */
    private List<Long> warehouseIds;

    /**
     * 条码
     */
    private List<Long> skuCodes;


    /**
     * 状态
     */
    private List<Integer> statusList;

    /**
     * 创建开始时间
     */
    private Date startAt;

    /**
     * 创建结束时间
     */
    private Date endAt;



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
