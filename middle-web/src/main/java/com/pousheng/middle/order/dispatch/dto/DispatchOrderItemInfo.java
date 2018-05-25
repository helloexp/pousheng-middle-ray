package com.pousheng.middle.order.dispatch.dto;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单派单信息
 * mpos订单到中台后可能会拆成仓库发货或门店发货，会存一个订单下的商品既有仓发也有门店发。
 * Created by songrenfei on 2017/12/22
 */
@Data
public class DispatchOrderItemInfo implements Serializable{

    private static final long serialVersionUID = -6197446559219078443L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * TODO review 子订单ID，这里先使用orderId
     */
    private List<Long> subOrderIds;

    /**
     * 开发平台店铺id
     */
    private Long openShopId;

    /**
     * 门店发货
     */
    private List<ShopShipment> shopShipments;


    /**
     * 仓库发货
     */
    private List<WarehouseShipment> warehouseShipments;


    /**
     * 无法派单的商品
     */
    private List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();


}
