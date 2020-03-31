package com.pousheng.middle.order.dto.reverseLogistic;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Date: 2019/06/06
 *
 * @author bernie
 */
@Data
public class ReverseExpressCriteria extends PagingCriteria implements java.io.Serializable {

    private static final long serialVersionUID = 4282263665769465196L;

    /**
     * 交接单号
     */
    private String transferOrderId;

    /**
     * 行号
     */
    private String lineNo;

    private List<String> statuses;

    /**
     * 状态
     */
    private String status;

    /**
     * 承运商编码
     */
    private String carrierCode;

    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 寄件人姓名
     */
    private String senderName;

    /**
     * 寄件人电话
     */
    private String senderMobile;

    /**
     * 是否有单
     */
    private Integer hasOrder;

    /**
     * 入库单号
     */
    private String instoreNo;

    /**
     * 店铺
     */
    private String shop;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 备注
     */
    private String buyerMemo;

    /**
     * 快递创建开始时间
     */
    private Date startAt;
    /**
     * 快递创建结束时间
     */
    private Date endAt;

}
