package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/1/16
 */
@Data
public class HkShipmentPosItem implements Serializable{

    private static final long serialVersionUID = -2142805920088639865L;

    private String sourcenetbillno; //来源单号 for 退货
    private String stockcode;  //仓库代码 for 退货
    private String matbarcode; //货号
    private Integer qty; //数量
    private String balaprice; //结算价
    private String couponprice;//平台优惠
}
