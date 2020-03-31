package com.pousheng.middle.consume.index.processor.impl.refund.builder;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 15:56<br/>
 */
@Data
public class RefundExtraDTO implements Serializable {
    private static final long serialVersionUID = -1460765291343146762L;

    /**
     * 处理完成时间
     */
    private Date handleDoneAt;
    /**
     * yyedi同步退货完成时间
     */
    private Date hkReturnDoneAt;
    /**
     * 换货发货单创建时间
     */
    private Date changeShipmentAt;
    /**
     * 丢件补发售后单创建时间
     */
    private Date lostShipmentAt;
    /**
     * 发货时间
     */
    private Date shipAt;
    /**
     * 确认收货时间
     */
    private Date confirmReceivedAt;
    /**
     * 退款时间
     */
    private Date refundAt;

    /**
     * 物流公司
     */
    private String shipmentCorpName;
    /**
     * 物流代码
     */
    private String shipmentCorpCode;
    /**
     * 物流单号
     */
    private String shipmentSerialNo;

}
