package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2018/1/16
 */
@Data
public class HkShipmentPosContent implements Serializable{

    private static final long serialVersionUID = 7713679282819923896L;

    private String channeltype = "b2c"; //订单来源类型, 是b2b还是b2c
    private String companyid; //实际发货账套id
    private String shopcode;  //实际发货店铺code for店发
    private String stockcode;  //实际发货店铺code for仓发
    private String voidstockcode;//实际发货账套的虚拟仓代码
    private String netcompanyid;  //线上店铺所属公司id
    private String netshopcode;  //线上店铺code
    private String netstockcode;//线上店铺所属公司的虚拟仓代码
    private String netbillno; //端点唯一订单号
    private String sourcebillno; //订单来源单号
    private String billdate;  //订单日期
    private String operator;  //线上店铺帐套操作人code
    private String remark; //备注
    private String islock ="0"; //"0":退货到宝胜仓 "1":退货到skx仓

    private HkShipmentPosInfo netsalorder;

    private List<HkShipmentPosItem> ordersizes;
}
