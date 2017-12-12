package com.pousheng.middle.web.order;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.ShipmentPreview;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 销售发货  和 换货发货 合并api
 * Created by songrenfei on 2017/7/6
 */
@RestController
@Slf4j
public class CreateShipments {

    @Autowired
    private AdminOrderReader adminOrderReader;
    @Autowired
    private Refunds refunds;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;


    /**
     * 待处理商品列表
     *
     * @param id   单据id
     * @param type 1 销售发货  2 换货发货
     * @return 商品信息
     */
    @RequestMapping(value = "/api/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> waitHandleSku(@RequestParam Long id, @RequestParam(defaultValue = "1") Integer type) {

        if (Objects.equals(1, type)) {
            return adminOrderReader.orderWaitHandleSku(id);
        }

        return refunds.refundWaitHandleSku(id);

    }


    /**
     * 发货预览
     *
     * @param id          单据id
     * @param data        json格式
     * @param warehouseId 仓库id
     * @param type        1 销售发货  2 换货发货
     * @return 订单信息
     */
    @RequestMapping(value = "/api/ship/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShipmentPreview> shipPreview(@RequestParam Long id,
                                                 @RequestParam("data") String data,
                                                 @RequestParam(value = "warehouseId") Long warehouseId,
                                                 @RequestParam(defaultValue = "1") Integer type) {

        Response<ShipmentPreview> response;
        if (Objects.equals(1, type)) {
            response = shipmentReadLogic.orderShipPreview(id, data);
        } else if (Objects.equals(2, type)) {
            response = shipmentReadLogic.changeShipPreview(id, data);
        } else {
            throw new JsonResponseException("invalid.type");
        }

        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }


        //封装发货仓及下单店铺信息
        ShipmentPreview shipmentPreview = response.getResult();

        //发货仓库信息
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        Warehouse warehouse = warehouseRes.getResult();
        shipmentPreview.setWarehouseId(warehouse.getId());
        shipmentPreview.setWarehouseName(warehouse.getName());
        //判断所选仓库是否数据下单店铺的账套
        if (!orderReadLogic.validateCompanyCode(warehouseId,shipmentPreview.getShopId())){
            throw new JsonResponseException("warehouse.must.be.in.one.company");
        }
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipmentPreview.getShopId());
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE,openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME,openShop);
        shipmentPreview.setErpOrderShopCode(shopCode);
        shipmentPreview.setErpOrderShopName(shopName);
        //如果订单extra表中存在绩效店铺编码，直接去shopOrderExtra中的绩效店铺编码

        shipmentPreview.setErpPerformanceShopCode(shopCode);
        shipmentPreview.setErpPerformanceShopName(shopName);

        //计算金额
        //发货单商品金额
        Long shipmentItemFee = 0L;
        //发货单总的优惠
        Long shipmentDiscountFee = 0L;
        //发货单总的净价
        Long shipmentTotalFee = 0L;
        //运费
        Long shipmentShipFee = 0L;
        //运费优惠
        Long shipmentShipDiscountFee = 0L;
        if (Objects.equals(1, type)) {
            //运费

            //判断运费是否已经加过
            if (!shipmentReadLogic.isShipmentFeeCalculated(id)) {

                ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
                shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee()==null?0:shopOrder.getOriginShipFee());
                shipmentShipDiscountFee = shipmentShipFee-Long.valueOf(shopOrder.getShipFee()==null?0:shopOrder.getShipFee());
            }
            shipmentPreview.setShipmentShipFee(shipmentShipFee);
        }
        List<ShipmentItem> shipmentItems = shipmentPreview.getShipmentItems();
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice()*shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = (shipmentItem.getSkuDiscount()==null?0:shipmentItem.getSkuDiscount()) + shipmentDiscountFee;
            shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
        }
        //发货单总金额(商品总净价+运费)
        Long shipmentTotalPrice= shipmentTotalFee+shipmentShipFee-shipmentShipDiscountFee;
        shipmentPreview.setShipmentItemFee(shipmentItemFee);
        shipmentPreview.setShipmentDiscountFee(shipmentDiscountFee);
        shipmentPreview.setShipmentTotalFee(shipmentTotalFee);
        shipmentPreview.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        shipmentPreview.setShipmentTotalPrice(shipmentTotalPrice);

        return Response.ok(shipmentPreview);
    }

}
