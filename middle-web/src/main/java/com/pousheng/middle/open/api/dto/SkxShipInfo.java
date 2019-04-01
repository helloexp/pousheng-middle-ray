package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/1下午6:01
 */
@Data
public class SkxShipInfo implements Serializable {

    private static final long serialVersionUID = -8431794061660831528L;
    /**
     * 中台发货单号
     */
    private String shipmentId;
    /**
     * SKX发货单号
     */
    private String erpShipmentId;
    /**
     * 物流公司代码
     */
    private String shipmentCorpCode;
    /**
     * 物流单号，可以用逗号隔开，可以有多个物流单号
     */
    private String shipmentSerialNo;
    /**
     * 发货时间
     */
    private String shipmentDate;

}
