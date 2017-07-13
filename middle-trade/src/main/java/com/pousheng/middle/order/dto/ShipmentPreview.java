package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发货预览页信息封装
 * Created by songrenfei on 2017/7/1
 */
@Data
public class ShipmentPreview extends OrderBasicInfo implements Serializable{

    private static final long serialVersionUID = 4997228803298504494L;

    //发货仓ID
    private Long warehouseId;

    //发货仓名称
    public String warehouseName;

    //绩效店铺名称
    private String erpPerformanceShopName;
    //绩效店铺编码
    private String erpPerformanceShopCode;
    //下单店铺名称
    private String erpOrderShopName;
    //下单店铺编码
    private String erpOrderShopCode;

    //发货商品信息
    List<ShipmentItem> shipmentItems;

}
