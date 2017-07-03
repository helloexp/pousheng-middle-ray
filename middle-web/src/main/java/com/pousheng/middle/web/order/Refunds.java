package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;
    @Autowired
    private SyncRefundLogic syncRefundLogic;


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
        return makeRefundDetail(refundId);
    }

    //编辑逆向单
    @RequestMapping(value = "/api/refund/{id}/edit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MiddleRefundDetail edit(@PathVariable(value = "id") Long refundId) {
        return makeRefundDetail(refundId);
    }




    /**
     * 换货发货的发货预览
     *
     * @param refundId 换货单id
     * @param data skuCode及数量 json格式
     * @param warehouseId          仓库id
     * @return 订单信息
     */
    @RequestMapping(value = "/api/refund/{id}/ship/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShipmentPreview> changeShipPreview(@PathVariable("id") Long refundId,
                                                       @RequestParam("data") String data,
                                                       @RequestParam(value = "warehouseId") Long warehouseId){
        Map<String, Integer> skuCodeAndQuantity = analysisSkuCodeAndQuantity(data);
        Refund refund = refundReadLogic.findRefundById(refundId);
        List<RefundItem>  refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        Warehouse warehouse = findWarehouseById(warehouseId);


        //订单基本信息
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());//orderRefund.getOrderId()为交易订单id
        List<Invoice> invoices = orderReadLogic.findInvoiceInfo(orderRefund.getOrderId());
        List<Payment> payments = orderReadLogic.findOrderPaymentInfo(orderRefund.getOrderId());
        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(orderRefund.getOrderId());


        //封装发货预览基本信息
        ShipmentPreview shipmentPreview  = new ShipmentPreview();
        shipmentPreview.setWarehouseId(warehouse.getId());
        shipmentPreview.setWarehouseName(warehouse.getName());
        shipmentPreview.setInvoices(invoices);
        if(!CollectionUtils.isEmpty(payments)){
            shipmentPreview.setPayment(payments.get(0));
        }
        shipmentPreview.setReceiverInfo(receiverInfo);
        shipmentPreview.setShopOrder(shopOrder);
        //封装发货预览商品信息
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithCapacity(refundChangeItems.size());
        for (RefundItem refundItem : refundChangeItems){
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setSkuCode(refundItem.getSkuCode());
            shipmentItem.setOutSkuCode(refundItem.getOutSkuCode());
            shipmentItem.setSkuName(refundItem.getSkuName());
            shipmentItem.setQuantity(skuCodeAndQuantity.get(refundItem.getSkuCode()));//替换为发货数量
            //todo 计算各种价格

            shipmentItems.add(shipmentItem);
        }
        shipmentPreview.setShipmentItems(shipmentItems);

        return Response.ok(shipmentPreview);
    }


    private Map<String, Integer> analysisSkuCodeAndQuantity(String data){
        Map<String, Integer> skuOrderIdAndQuantity = mapper.fromJson(data, mapper.createCollectionType(HashMap.class, String.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuCodeAndQuantity:{}",data);
            throw new JsonResponseException("sku.quantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }



    /**
     * 创建逆向单
     * @param submitRefundInfo 提交信息
     * @return 逆向单id
     */
    @RequestMapping(value = "/api/refund/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createRefund(@RequestBody SubmitRefundInfo submitRefundInfo){
        //验证提交信息是否有效
        //订单是否有效
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //发货单是否有效
        Shipment shipment = shipmentReadLogic.findShipmentById(submitRefundInfo.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //申请数量是否有效
        RefundItem refundItem = checkRefundQuantity(submitRefundInfo,shipmentItems);

        if(Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(),submitRefundInfo.getRefundType())){
            //换货数量是否有效
            RefundItem changeItem = checkChangeQuantity(submitRefundInfo);
        }



        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        if(Objects.equals(submitRefundInfo.getOperationType(),1)){
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }else {
            refund.setStatus(MiddleRefundStatus.WAIT_SYNC_HK.getValue());
        }
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setFee(submitRefundInfo.getFee());
        refund.setRefundType(submitRefundInfo.getRefundType());

        Map<String,String> extraMap = Maps.newHashMap();

        RefundExtra refundExtra = new RefundExtra();

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        refundExtra.setShipmentId(shipment.getId());
        //非仅退款则验证仓库是否有效、物流信息是否有效
        if(!Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())){
            Warehouse warehouse = findWarehouseById(submitRefundInfo.getWarehouseId());
            refundExtra.setWarehouseId(warehouse.getId());
            refundExtra.setWarehouseName(warehouse.getName());
            refundExtra.setShipmentCorpCode(submitRefundInfo.getShipmentCorpCode());
            refundExtra.setShipmentSerialNo(submitRefundInfo.getShipmentSerialNo());
            refundExtra.setShipmentCorpName(submitRefundInfo.getShipmentCorpName());
        }

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(refundItem)));
        extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(changeItem)));

        refund.setExtra(extraMap);

        //打标
        Map<String,String> tagMap = Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.MANUAL.value()));
        refund.setTags(tagMap);




        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(submitRefundInfo.getOrderId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }

        return rRefundRes.getResult();
    }






    /**
     * 换货单待发货商品列表 for 手动生成发货单流程的选择仓库页面
     * @param refundId 换货单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 退货数量 - 已发货数量
     */
    @RequestMapping(value = "/api/refund/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> waitHandleSku(@PathVariable("id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);

        List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundChangeItems.size());
        for (RefundItem refundItem : refundChangeItems){
            WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
            waitShipItemInfo.setSkuCode(refundItem.getSkuCode());
            waitShipItemInfo.setOutSkuCode(refundItem.getSkuCode());
            waitShipItemInfo.setWaitHandleNumber(refundItem.getQuantity()-refundItem.getAlreadyHandleNumber());
            waitShipItemInfos.add(waitShipItemInfo);
        }
        return waitShipItemInfos;
    }



    /**
     * 同步售后单到恒康
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/sync/hk",method = RequestMethod.PUT)
    public void syncHkRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundLogic.syncRefundToHk(refund);
        if(!syncRes.isSuccess()){
            log.error("sync refund(id:{}) to hk fail,error:{}",refundId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }



    /**
     * 取消售后单
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel",method = RequestMethod.PUT)
    public void cancleRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> cancelRes = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.CANCEL.toOrderOperation());
        if(!cancelRes.isSuccess()){
            log.error("cancel refund(id:{}) fail,error:{}",refundId,cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
    }


    /**
     * 同步售后单取消状态到恒康
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel/sync/hk",method = RequestMethod.PUT)
    public void syncHkCancelRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundLogic.syncRefundCancelToHk(refund);
        if(!syncRes.isSuccess()){
            log.error("sync cancel refund(id:{}) to hk fail,error:{}",refundId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
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

    private MiddleRefundDetail makeRefundDetail(Long refundId){

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
            refundDetail.setOrderShipments(shipmentReadLogic.findByAfterOrderIdAndType(refundId));
        }

        return  refundDetail;

    }

}
