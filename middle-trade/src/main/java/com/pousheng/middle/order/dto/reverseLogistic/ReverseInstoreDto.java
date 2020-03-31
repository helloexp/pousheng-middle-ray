package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.Data;

import java.io.Serializable;

/**
 * @author bernie
 * @date 2019-06-03
 * Desc: 退货货物入库单信息
 * Date: 2019-06-03
 */
@Data
public class ReverseInstoreDto extends  ReverseLogisticBase implements Serializable {


    private static final long serialVersionUID = -8245321932692360773L;

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


    private ReverseLogisticTime timeInfo;

    /**
     * 异常大类
     */
    private String rtxAnomalyBig;

    /**
     * 异常小类
     */
    private String rtxAnomalySmall;

    /**
     * 入库商品信息
     */
    private WaybillItemDto waybillItemDto;

    /**
     * 操作人
     */
    private String createdBy;

    /**
     * 买家备注
     */
    private String customerNote;



}
