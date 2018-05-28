package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 取消售后单
 * Created by songrenfei on 2018/4/1
 */
@Data
public class CancelOutRefundInfo implements Serializable{


    private static final long serialVersionUID = 140053268739456033L;
    private String outAfterSaleOrderId; //云聚售后单id
    private String channel; //渠道 云聚默认填:YJ
    private String buyerNote;//买家备注
    private String sellerNote;//商家备注
    private String applyAt; //申请时间 yyyyMMdd HHmmss


}
