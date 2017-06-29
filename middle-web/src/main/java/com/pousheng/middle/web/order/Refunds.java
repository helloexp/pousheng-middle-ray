package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.dto.MiddleRefundDetail;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
public class Refunds {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;


    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<RefundPaging> findBy(RefundCriteria criteria) {

        Response<Paging<RefundPaging>> pagingRes = refundReadLogic.refundPaging(criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging refund by criteria:{} fail,error:{}",criteria,pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MiddleRefundDetail detail(@PathVariable(value = "id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        refundDetail.setRefundItems(refundReadLogic.findRefundItems(refund));
        refundDetail.setRefundExtra(refundExtra);

        //如果为换货,则封装发货信息（换货的发货单）
        if(Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())&& refund.getStatus()> MiddleRefundStatus.WAIT_SHIP.getValue()){
            refundDetail.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
            refundDetail.setOrderShipments(shipmentReadLogic.findByOrderIdAndType(refundId, ShipmentType.EXCHANGE_SHIP));
        }

        return refundDetail;
    }

    @RequestMapping(value = "/api/refund/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createRefund(@RequestBody SubmitRefundInfo submitRefundInfo){
        //验证提交信息是否有效
        //订单是否有效
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //发货单是否有效
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentById(submitRefundInfo.getOrderShipmentId());
        Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //申请数量是否有效
        RefundItem refundItem = checkRefundQuantity(submitRefundInfo,shipmentItems);
        //换货数量是否有效
        RefundItem changeItem = checkChangeQuantity(submitRefundInfo);



        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setFee(submitRefundInfo.getFee());
        refund.setRefundType(submitRefundInfo.getRefundType());

        Map<String,String> extraMap = Maps.newHashMap();

        RefundExtra refundExtra = new RefundExtra();

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        refundExtra.setShipmentId(orderShipment.getId());
        //非仅退款则验证仓库是否有效、物流信息是否有效
        if(!Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())){
            Warehouse warehouse = findWarehouseById(submitRefundInfo.getWarehouseId());
            refundExtra.setWarehouseId(warehouse.getId());
            refundExtra.setWarehouseName(warehouse.getName());
            //todo 物流名称
            refundExtra.setShipmentCorpCode(submitRefundInfo.getShipmentCorpCode());
            refundExtra.setShipmentSerialNo(submitRefundInfo.getShipmentSerialNo());
        }

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(refundItem)));
        extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(changeItem)));

        refund.setExtra(extraMap);



        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(submitRefundInfo.getOrderId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }

        return rRefundRes.getResult();
    }

    private RefundItem checkRefundQuantity(SubmitRefundInfo submitRefundInfo,List<ShipmentItem> shipmentItems){
        for (ShipmentItem shipmentItem : shipmentItems){
            if(Objects.equals(submitRefundInfo.getRefundSkuCode(),shipmentItem.getSkuCode())){
                Integer availableQuantity = shipmentItem.getQuantity()-shipmentItem.getRefundQuantity();
                if(submitRefundInfo.getRefundQuantity()>availableQuantity){
                    log.error("refund quantity:{} gt available quantity:{}",submitRefundInfo.getRefundQuantity(),availableQuantity);
                    throw new JsonResponseException("refund.quantity.invalid");
                }
                RefundItem refundItem = new RefundItem();
                BeanMapper.copy(refundItem,shipmentItem);
                refundItem.setQuantity(submitRefundInfo.getRefundQuantity());
                return refundItem;
            }
        }
        log.error("refund sku code:{} invalid",submitRefundInfo.getRefundSkuCode());
        throw new JsonResponseException("check.refund.quantity.fail");

    }

    private RefundItem checkChangeQuantity(SubmitRefundInfo submitRefundInfo){
        if(!Objects.equals(submitRefundInfo.getRefundQuantity(),submitRefundInfo.getChangeQuantity())){
            log.error("refund quantity:{} not equal change quantity:{}",submitRefundInfo.getRefundQuantity(),submitRefundInfo.getChangeQuantity());
            throw new JsonResponseException("refund.quantity.not.equal.change.quantity");
        }
        //todo 封装换货商品信息
        RefundItem refundItem = new RefundItem();
        refundItem.setQuantity(submitRefundInfo.getChangeQuantity());
        refundItem.setSkuCode(submitRefundInfo.getChangeSkuCode());
        return refundItem;

    }

    private Warehouse findWarehouseById(Long warehouseId){

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

}
