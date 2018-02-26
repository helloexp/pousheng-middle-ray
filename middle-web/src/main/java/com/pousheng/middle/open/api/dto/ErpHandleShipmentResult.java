package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/28
 */
@Data
public class ErpHandleShipmentResult implements Serializable{


    private static final long serialVersionUID = 4932812796750804333L;
    /**
     * erp发货单号
     */
    private String erpShipmentId;

    /**
     * 中台发货单号
     */
    private Long ecShipmentId;

    /**
     * 处理结果
     */
    private Boolean success;

    /**
     * 错误信息 （当success 为false 时 error需要提供错误提示）
     */
    private String error;
}
