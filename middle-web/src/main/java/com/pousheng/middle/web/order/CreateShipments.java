package com.pousheng.middle.web.order;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.ShipmentPreview;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
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

        Response<WarehouseCompanyRule> ruleRes = shipmentReadLogic.findCompanyRuleByWarehouseCode(warehouse.getCode());
        if (!ruleRes.isSuccess()) {
            log.error("find warehouse company rule by company code:{} fail,error:{}", warehouse.getCode(), ruleRes.getError());
            throw new JsonResponseException(ruleRes.getError());
        }

        WarehouseCompanyRule companyRule = ruleRes.getResult();
        shipmentPreview.setErpOrderShopCode(companyRule.getShopId());
        shipmentPreview.setErpOrderShopName(companyRule.getShopName());
        shipmentPreview.setErpPerformanceShopCode(companyRule.getShopId());
        shipmentPreview.setErpPerformanceShopName(companyRule.getShopName());

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
            if (!isShipmentFeeCalculated(id)) {

                ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
                shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee()==null?0:shopOrder.getOriginShipFee());
                shipmentShipDiscountFee = shipmentShipFee-Long.valueOf(shopOrder.getShipFee()==null?0:shopOrder.getShipFee());
            }
            shipmentPreview.setShipmentShipFee(shipmentShipFee);
        }
        List<ShipmentItem> shipmentItems = shipmentPreview.getShipmentItems();
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice()*shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
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


    /**
     * 判断是否存在有效的发货单
     *
     * @param shopOrderId
     * @return true:已经计算过发货单,false:没有计算过发货单
     */
    private boolean isShipmentFeeCalculated(long shopOrderId) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new JsonResponseException("find.shipment.failed");
        }
        //获取有效的销售发货单
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue())).
                filter(it -> Objects.equals(it.getType(), ShipmentType.SALES_SHIP.value())).collect(Collectors.toList());
        int count = 0;
        for (Shipment shipment : shipments) {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (shipmentExtra.getShipmentShipFee() > 0) {
                count++;
            }
        }
        //如果已经有发货单计算过运费,返回true
        if (count > 0) {
            return true;
        }
        return false;
    }


}
