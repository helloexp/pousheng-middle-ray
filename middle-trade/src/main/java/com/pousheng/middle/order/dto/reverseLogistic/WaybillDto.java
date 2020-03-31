package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author bernie
 * @date 2019/6/3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaybillDto implements Serializable {

    private static final long serialVersionUID = 3049533361661183374L;

    /**
     * 承运商编码
     */
    private String carrierCode;

    /**
     * 承运方名称
     */
    private String carrierName;

    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 支付类型（支付前，货到付款）
     */
    private Integer paidAfterDelivery;


    private SenderDto senderInfo;

    /**
     * 发货方
     */
    private String shipper;

    /**
     * 付款方
     */
    private String payer;

    /**
     * 备注
     */
    private String buyerMemo;


    /**
     * 付款金额（分）
     */
    private Long fee;

}
