package com.pousheng.middle.order.dto;

import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/16
 */
@Data
public class MiddleOrderCriteria extends PagingCriteria implements Serializable {

    private static final long serialVersionUID = -4118002783835337285L;


    /**
     * 订单id
     */
    private String id;

    /**
     * 订单号
     */
    private String orderCode;

    /**
     * 外部订单id
     */
    private String outId;

    /**
     * 订单来源
     *
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
     * 店铺ids，用于过滤用户可操作的店铺
     */
    private List<Long> shopIds;

    /**
     * 状态
     */
    private List<Integer> status;
    /**
     * 状态,用,分割
     */
    private String statusStr;
    /**
     * 该字段用于存放手机号
     */
    private String outBuyerId;
    private String mobile;
    /**
     * 借用字段，用来查询该订单是否含有赠品
     */
    private Long companyId;//赠品订单companyId=1

    /**
     * 处理状态
     */
    private Integer handleStatus;

    /**
     * 排除外部订单来源
     */
    private String excludeOutFrom;

    /**
     * 排除订单类型
     */
    private Integer excludeType;

    /**
     *  排除买家备注信息（模糊查询）
     */
    private String buyerNote;
    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    @Override
    public void formatDate() {
        if (outCreatedStartAt != null && outCreatedEndAt != null) {
            if (outCreatedStartAt.equals(outCreatedEndAt)) {
                outCreatedEndAt = new DateTime(outCreatedEndAt.getTime()).plusDays(1).minusSeconds(1).toDate();
            }
        }
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
        if (StringUtils.hasText(statusStr)) {
            this.status = Splitters.splitToInteger(statusStr, Splitters.COMMA);
        }
    }
    
    public void setBuyerNote(String buyerNote){
        this.buyerNote = buyerNote.trim();
        if (StringUtils.hasText(buyerNote)){
            String[] sd = buyerNote.split(",|，");
            StringBuilder sb = new StringBuilder();
            for(String remark:sd){
                if(StringUtils.hasText(remark)){
                   sb = sb.append(remark).append("|"); 
                }
            }
            if(StringUtils.hasText(sb)){
                this.buyerNote = sb.substring(0,sb.length()-1); 
            }else{
                this.buyerNote = "";
            }
        }
    }

}
