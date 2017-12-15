package com.pousheng.middle.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 门店服务信息
 * Created by songrenfei on 2017/12/6
 */
@Data
public class ShopServerInfo implements Serializable{

    private static final long serialVersionUID = 9077504691689733436L;

    //虚拟店code
    private String virtualShopCode;
    //虚拟店名称
    private String virtualShopName;

    //退货仓id
    private Long returnWarehouseId;
    //退货仓编码
    private String returnWarehouseCode;
    //退货仓名称
    private String returnWarehouseName;
}
