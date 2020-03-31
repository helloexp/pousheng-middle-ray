package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author bernie
 * @date: 2019-06-03
 * 无头件信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReverseHeadlessDto extends  ReverseLogisticBase implements Serializable {

    private static final long serialVersionUID = -7174417306258443917L;


    /**
     * 无头件单号
     */
    private String headlessNo;


    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 平台销售单号
     */
    private String platformNo;

    /**
     * 客户名称
     */
    private String customer;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 唯一码
     */
    private String uniqueNo;

    /**
     * 原因
     */
    private String reason;

    /**
     * 关联ASN:
     */
    private String relateAsn;

    /**
     * 盘盈入库单
     */
    private String inventoryProfitNo;

    /**
     * 出货方式
     */
    private String shipMode;

    /**
     * 出货快递公司
     */
    private String shipCompany;

    /**
     * 出货快递单号
     */
    private String shipExpressNo;



    WaybillItemDto waybillItemDto;

    private ReverseLogisticTime timeInfo;



}
