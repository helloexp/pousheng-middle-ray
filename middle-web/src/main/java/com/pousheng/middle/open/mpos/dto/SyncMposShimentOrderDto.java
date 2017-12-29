package com.pousheng.middle.open.mpos.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by penghui on 2017/12/26
 * 同步mpos的参数
 */
@Data
public class SyncMposShimentOrderDto implements Serializable{

    private static final long serialVersionUID = -8428434966527970853L;

    //发货单ID
    private Long shipmentId;
    //子订单ID
    List<Long> skuOrderIds;
    //外部订单id
    private String orderId;
    //发货店编码（仓库）
    private Long ShopCode;
    //发货店名称（仓库）
    private String ShopName;
    //发货类型 0店铺发货 1店铺自提 2仓发
    private String shipmentWay;


}
