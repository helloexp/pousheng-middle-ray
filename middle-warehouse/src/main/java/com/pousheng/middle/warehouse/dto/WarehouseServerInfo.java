package com.pousheng.middle.warehouse.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 仓库服务信息
 *
 */
@Data
public class WarehouseServerInfo implements Serializable{


    private static final long serialVersionUID = 7590222831051741550L;
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
