package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 发货单发货时间
 * Created by songrenfei on 2018/1/17
 */
@Data
public class HkShimentDoneInfo implements Serializable{

    private static final long serialVersionUID = -3692340853940729896L;

    //恒康订单号
    private String netbillno;

    //收货日期
    private String receiptdate;
}
