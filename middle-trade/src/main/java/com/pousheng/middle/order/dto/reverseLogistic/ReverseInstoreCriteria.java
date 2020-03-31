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
public class ReverseInstoreCriteria extends PagingCriteria implements java.io.Serializable {

    private static final long serialVersionUID = 4282263665769465196L;

    private List<String> statuses;

    /**
     * 状态
     */
    private String status;

    private Long id;

    /**
     * 退货入库单单号
     */
    private String instoreNo;

    /**
     * 退货入库明细行
     */
    private String instoreDetailNo;

    /**
     * ERP单号
     */
    private String erpNo;

    /**
     * 平台销售单单号
     */
    private String platformNo;

    /**
     * 承运商运单号
     */
    private String carrierExpressNo;

    /**
     * 实际快递单号
     */
    private String realExpressNo;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 店铺
     */
    private String shop;

    /**
     * 倒仓开始时间
     */
    private Date arriveWmsStartAt;
    /**
     * 倒仓结束时间
     */
    private Date arriveWmsEndAt;
    /**
     * 客服备注
     */
    private String customerNote;

}
