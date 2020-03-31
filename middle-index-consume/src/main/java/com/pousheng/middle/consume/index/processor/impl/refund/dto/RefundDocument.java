package com.pousheng.middle.consume.index.processor.impl.refund.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-21 20:49<br/>
 */
@Data
public class RefundDocument implements Serializable {
    private static final long serialVersionUID = -8308380847615169794L;

    private Long id;
    private String refundCode;
    private String releOrderCode;
    private Integer status;
    private Integer refundType;
    private Integer completeReturn;
    private String returnSerialNo;
    private String shipmentSerialNo;
    private String shipmentCorpCode;
    private String shipmentCorpName;
    private Long shopId;
    private String shopName;
    private String buyerName;

    private String updatedAt;
    private String createdAt;
    private String refundAt;
    private String receivedDate;
    private String hkReturnDoneAt;
}
