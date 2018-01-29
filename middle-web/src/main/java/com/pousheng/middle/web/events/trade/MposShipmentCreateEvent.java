package com.pousheng.middle.web.events.trade;

import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.util.List;

/**
 * Created by penghui on 2018/1/26
 */
@Data
public class MposShipmentCreateEvent {

    /**
     * 发货单
     */
    private Shipment shipment;

    /**
     * 订单
     */
    private ShopOrder shopOrder;

    /**
     * 商品数量
     */
    private List<SkuCodeAndQuantity> skuCodeAndQuantities;

    /**
     * 类型 1.仓发 2.店发
     */
    private Integer type;

    /**
     * 针对生成发货单同步
     * @param shipment
     * @param type
     */
    public MposShipmentCreateEvent(Shipment shipment,Integer type){
        this.shipment = shipment;
        this.type = type;
    }

    /**
     * 针对无法派单商品
     * @param shopOrder
     * @param skuCodeAndQuantities
     */
    public MposShipmentCreateEvent(ShopOrder shopOrder,List<SkuCodeAndQuantity> skuCodeAndQuantities){
        this.shopOrder = shopOrder;
        this.skuCodeAndQuantities = skuCodeAndQuantities;
    }
}
