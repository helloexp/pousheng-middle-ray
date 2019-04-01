package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2018/1/16
 */
@Data
public class HkSaleRefuseContent implements Serializable{

    private static final long serialVersionUID = 7713679282819923896L;
    //线上店铺所属公司id
    private String netcompanyid;
    //线上店铺code
    private String netshopcode;
    //端点唯一订单号
    private String netbillno;
    //订单来源单号
    private String sourcebillno;
    //备注
    private String remark;
}
