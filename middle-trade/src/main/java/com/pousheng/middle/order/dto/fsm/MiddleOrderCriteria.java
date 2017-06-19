package com.pousheng.middle.order.dto.fsm;

import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/16
 */
@Data
public class MiddleOrderCriteria extends PagingCriteria implements Serializable{

    private static final long serialVersionUID = -4118002783835337285L;

    /**
     * 外部订单id
     */
    private String outId;

    /**
     * 订单来源
     * @see com.pousheng.middle.order.enums.OrderSource
     */
    private Integer type;
    /**
     * 下单开始时间
     */
    private Date outCreatedStartAt;

    /**
     * 下单结束
     */
    private Date outCreatedEndAt;

    /**
     * 买家名称
     */
    private String buyerName;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺id
     */
    private Long shopId;


    /**
     * 状态
     */
    private List<Integer> status;

    /**
     * 状态,用,分割
     */
    private String statusStr;



    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    @Override
    public void formatDate(){
        if(outCreatedStartAt != null && outCreatedEndAt != null){
            if(outCreatedStartAt.equals(outCreatedEndAt)){
                outCreatedEndAt=new DateTime(outCreatedEndAt.getTime()).plusDays(1).minusSeconds(1).toDate();
            }
        }
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
        if (StringUtils.hasText(statusStr)) {
            this.status = Splitters.splitToInteger(statusStr, Splitters.COMMA);
        }
    }

}
