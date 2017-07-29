package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.hksyc.component.SycHkShipmentOrderApi;
import com.pousheng.middle.hksyc.dto.HkResponseHead;
import com.pousheng.middle.hksyc.dto.trade.*;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.HKInvoiceType;
import com.pousheng.middle.order.enums.HkPayType;
import com.pousheng.middle.order.enums.MiddleInvoiceType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.pousheng.middle.order.enums.MiddlePayType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 同步恒康发货单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentLogic {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycHkShipmentOrderApi sycHkShipmentOrderApi;
    @Autowired
    private SycHkOrderCancelApi sycHkOrderCancelApi;
    @Autowired
    private WarehouseReadService warehouseReadService;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步发货单到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentToHk(Shipment shipment) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            //同步恒康
            List<SycHkShipmentOrderDto> list = this.makeShipmentOrderDtoList(shipment);
            SycShipmentOrderResponse response = this.makeSycShipmentOrderResponse(sycHkShipmentOrderApi.doSyncShipmentOrder(list));
            //解析返回结果
            HkResponseHead head = response.getHead();
            SycHkShipmentOrderResponseBody orderBody = response.getOrderBody();
            if (Objects.equals(head.getCode(), 0)) {
                Shipment newStatusShipment = shipmentReadLogic.findShipmentById(shipment.getId());
                //更新发货单的状态
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(newStatusShipment, syncOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateStatusRes.getError());
                }
                //冗余恒康发货单号
                Shipment update = new Shipment();
                update.setId(shipment.getId());
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                shipmentExtra.setOutShipmentId(orderBody.getErpOrderNo());
                Map<String, String> extraMap = shipment.getExtra();
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shipmentExtra));
                update.setExtra(extraMap);
                shipmentWiteLogic.update(update);
            } else {
                Shipment newStatusShipment = shipmentReadLogic.findShipmentById(shipment.getId());
                //更新发货单的状态
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(newStatusShipment, syncOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateStatusRes.getError());
                }
                return Response.fail("sync.hk.shipment.fail");
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            Shipment newStatusShipment = shipmentReadLogic.findShipmentById(shipment.getId());
            //更新发货单的状态
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(newStatusShipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            return Response.fail("sync.hk.shipment.fail");
        }

    }

    /**
     * 同步发货单取消到恒康
     *
     * @param shipment 发货单
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentCancelToHk(Shipment shipment) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            String response = sycHkOrderCancelApi.doCancelOrder(shipmentExtra.getErpOrderShopCode(), shipment.getId(), 0);
            HkResponseHead head = this.makeHkResponseHead(response);
            //head返回0代表成功
            if (Objects.equals(head.getCode(), 0)) {
                OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
                if (!updateStatusRes1.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                    return Response.fail(updateStatusRes.getError());
                }
            } else {
                OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
                Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
                if (!updateStatusRes1.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                    return Response.fail(updateStatusRes1.getError());
                }
                return Response.fail("sync.hk.cancel.shipment.failed");
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e1) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e1.getMessage());
            OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
            Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
            if (!updateStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                return Response.fail(updateStatusRes1.getError());
            }
            return Response.fail("sync.hk.cancel.shipment.failed");
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
            Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
            if (!updateStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                return Response.fail(updateStatusRes1.getError());
            }
            return Response.fail("sync.hk.cancel.shipment.failed");
        }
    }

    /**
     * 发货单同步恒康参数组装
     *
     * @param shipment
     * @return
     */
    public List<SycHkShipmentOrderDto> makeShipmentOrderDtoList(Shipment shipment) {
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        //获取发货单信息
        SycHkShipmentOrder tradeOrder = this.getSycHkShipmentOrder(shipment, shipmentDetail);
        //获取发货单地址信息
        SycHkUserAddress userAddress = this.getSycHkUserAddress(shipmentDetail);

        SycHkShipmentOrderDto dto = new SycHkShipmentOrderDto();
        dto.setTradeOrder(tradeOrder);
        dto.setUserAddress(userAddress);
        List<SycHkShipmentOrderDto> list = Lists.newArrayList();
        list.add(dto);
        return list;
    }

    /**
     * 组装发货信息
     *
     * @param shipment
     * @param shipmentDetail
     * @return
     */
    private SycHkShipmentOrder getSycHkShipmentOrder(Shipment shipment, ShipmentDetail shipmentDetail) {
        SycHkShipmentOrder tradeOrder = new SycHkShipmentOrder();
        //中台主订单-发货单id
        tradeOrder.setOrderNo(String.valueOf(shipment.getId()));
        //会员账号昵称
        tradeOrder.setBuyerNick(shipmentDetail.getReceiverInfo().getReceiveUserName());
        //订单总金额
        tradeOrder.setOrderMon(Math.toIntExact(shipmentDetail.getShipmentExtra().getShipmentItemFee()));
        //订单总运费
        tradeOrder.setFeeMon(Math.toIntExact(shipmentDetail.getShipmentExtra().getShipmentShipFee()));
        //买家应付金额
        tradeOrder.setRealMon(Math.toIntExact(shipmentDetail.getShipmentExtra().getShipmentTotalFee()));
        //买家留言
        tradeOrder.setBuyerRemark(shipmentDetail.getShopOrder().getBuyerNote());
        //第三方支付流水号-可以不传
        tradeOrder.setPaymentSerialNo("");
        // 订单状态默认为处理中
        tradeOrder.setOrderStatus(String.valueOf(2));
        //订单创建时间
        tradeOrder.setCreatedDate(formatter.print(shipment.getCreatedAt().getTime()));
        //订单修改时间
        tradeOrder.setUpdatedDate(formatter.print(shipment.getUpdatedAt().getTime()));
        //支付类型在中台是1:在线支付,2:货到付款,同步给恒康时需要变为0:在线支付,1:货到付款
        tradeOrder.setPaymentType(String.valueOf(this.getHkPayType(shipmentDetail).getValue()));
        //发票类型(中台1:普通发票,2:增值税发票3:电子发票),恒康(1:普通发票,2.电子发票,3.增值税发票)
        tradeOrder.setInvoiceType(String.valueOf(this.getHkInvoiceType(shipmentDetail).getValue()));
        //发票抬头
        tradeOrder.setInvoice(shipmentDetail.getInvoices().get(0).getTitle());
        //TODO 税号
        tradeOrder.setTaxNo("");
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        //下单店铺编码
        tradeOrder.setShopId(shipmentExtra.getErpOrderShopCode());
        //绩效店铺编码
        tradeOrder.setPerformanceShopId(shipmentExtra.getErpPerformanceShopCode());
        //todo 发货仓库id
        tradeOrder.setStockId(String.valueOf(shipmentExtra.getWarehouseId()));
        //获取发货单中对应的sku列表
        List<SycHkShipmentItem> items = this.getSycHkShipmentItems(shipment, shipmentDetail);
        tradeOrder.setItems(items);
        return tradeOrder;
    }

    /**
     * 组装发货单中sku订单信息
     *
     * @param shipment
     * @param shipmentDetail
     * @return
     */
    private List<SycHkShipmentItem> getSycHkShipmentItems(Shipment shipment, ShipmentDetail shipmentDetail) {
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        List<SycHkShipmentItem> items = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (ShipmentItem shipmentItem : shipmentItems) {
            SycHkShipmentItem item = new SycHkShipmentItem();
            //发货单id(恒康:中台主订单号)
            item.setOrderNo(String.valueOf(shipment.getId()));
            //(恒康:中台子订单号),这里拼接了发货单id与skuId
            item.setOrderSubNo(String.valueOf(shipment.getId()) + "-" + String.valueOf(shipmentItem.getSkuOrderId()));
            //中台skuCode
            item.setBarcode(shipmentItem.getOutSkuCode());
            //购买数量--对应中台发货单sku发货数量
            item.setNum(shipmentItem.getQuantity());
            //优惠金额--中台折扣/100
            item.setPreferentialMon(shipmentItem.getSkuDiscount() / 100);
            //销售单价(减去所有的优惠(优惠需要按比例计算))
            item.setSalePrice((shipmentItem.getCleanPrice()) / 100);
            //总价(销售价格*数量)
            item.setTotalPrice((shipmentItem.getCleanFee()) / 100);
            //赠品(1),非赠品(2)默认填写非赠品
            item.setIsGifts(2);
            items.add(item);
        }
        return items;
    }

    /**
     * 恒康发货单买家地址信息
     *
     * @param shipmentDetail
     * @return
     */
    private SycHkUserAddress getSycHkUserAddress(ShipmentDetail shipmentDetail) {
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        SycHkUserAddress userAddress = new SycHkUserAddress();
        //收货信息中没有国别信息,默认中国
        userAddress.setCountry("中国");
        //城市
        userAddress.setCity(receiverInfo.getCity());
        //省
        userAddress.setProvince(receiverInfo.getProvince());
        //邮编
        userAddress.setZipCode(receiverInfo.getPostcode());
        //具体地址
        userAddress.setAddressDetail(receiverInfo.getDetail());
        //手机号
        userAddress.setMobile(receiverInfo.getMobile());
        //电话
        userAddress.setTel(receiverInfo.getPhone());
        //联系人
        userAddress.setContact(receiverInfo.getReceiveUserName());
        //邮箱
        userAddress.setEmail(receiverInfo.getEmail());
        //区县
        userAddress.setDistrict(receiverInfo.getRegion());
        return userAddress;
    }

    /**
     * 组装同步恒康发货单返回参数(包括请求头和请求体)
     *
     * @param response
     * @return
     */
    private SycShipmentOrderResponse makeSycShipmentOrderResponse(String response) throws IOException {
        SycShipmentOrderResponse sycShipmentOrderResponse = new SycShipmentOrderResponse();

        Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);

        if (CollectionUtils.isEmpty(responnseMap)) {
            log.error("sync hk and shipmentResponseMap is null");
            throw new ServiceException("shipment.responseMap.is.null");
        }
        if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
            log.error("shipmentResponseMap not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
            throw new ServiceException("shipment.responseMap.head.is.null");
        }
        //获取响应头
        HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);
        sycShipmentOrderResponse.setHead(head);
        //如果存在响应body,则返回响应body
        if (responnseMap.containsKey(TradeConstants.HK_SHIPMENT_ORDER_BODY)) {
            SycHkShipmentOrderResponseBody orderBody = mapper.fromJson(responnseMap.get(TradeConstants.HK_SHIPMENT_ORDER_BODY), SycHkShipmentOrderResponseBody.class);
            sycShipmentOrderResponse.setOrderBody(orderBody);
        }
        return sycShipmentOrderResponse;
    }

    /**
     * 组装取消恒康发货单返回参数(只有请求头)
     *
     * @param response
     * @return
     */
    private HkResponseHead makeHkResponseHead(String response) {
        HkResponseHead responseHead = new HkResponseHead();
        try {
            Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);
            if (CollectionUtils.isEmpty(responnseMap)) {
                log.error("sync cancel hk and shipmentResponseMap is null");
                throw new ServiceException("shipment.responseMap.is.null");
            }
            if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
                log.error("shipmentResponseMap not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
                throw new ServiceException("shipment.responseMap.head.is.null");
            }
            HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseHead;
    }

    /**
     * 中台支付类型映射为恒康支付类型
     *
     * @param shipmentDetail
     * @return
     */
    private HkPayType getHkPayType(ShipmentDetail shipmentDetail) {
        MiddlePayType middlePayType = MiddlePayType.fromInt(shipmentDetail.getShopOrder().getPayType());
        if (Arguments.isNull(middlePayType)) {
            log.error("shipment(id:{})invalid", shipmentDetail.getShipment().getId());
            throw new ServiceException("shoporder.payType.invalid");
        }
        switch (middlePayType) {
            case ONLINE_PAY:
                return HkPayType.HK_ONLINE_PAY;
            case CASH_ON_DELIVERY:
                return HkPayType.HK_CASH_ON_DELIVERY;
            default:
                log.error("shippment(id:{}) invalid", shipmentDetail.getShipment().getId());
                throw new ServiceException("shoporder.payType.invalid");
        }
    }

    /**
     * 映射中台发票类型到恒康
     *
     * @param shipmentDetail
     * @return
     */
    private HKInvoiceType getHkInvoiceType(ShipmentDetail shipmentDetail) {
        Map<String, String> invoiceTypeMap = shipmentDetail.getInvoices().get(0).getDetail();
        if (!invoiceTypeMap.containsKey(TradeConstants.INVOICE_TYPE)) {
            log.error("can not find invoice type,shipmentId is({})", shipmentDetail.getShipment().getId());
            throw new ServiceException("find.invoice.failed");
        }
        Integer invoiceType = Integer.valueOf(invoiceTypeMap.get(TradeConstants.INVOICE_TYPE));
        MiddleInvoiceType middleInvoiceType = MiddleInvoiceType.fromInt(invoiceType);
        switch (middleInvoiceType) {
            case PLAIN_INVOICE:
                return HKInvoiceType.PLAIN_INVOICE;
            case VALUE_ADDED_TAX_INVOICE:
                return HKInvoiceType.VALUE_ADDED_TAX_INVOICE;
            case ELECTRONIC_INVOCE:
                return HKInvoiceType.ELECTRONIC_INVOCE;
            default:
                log.error("shippment(id:{}) invalid", shipmentDetail.getShipment().getId());
                throw new ServiceException("shoporder.invoice.invalid");
        }
    }
    /**
     * 获取恒康
     * @param warehouseId
     * @return
     */
    private String getHkWarehouseCodeById(long warehouseId){
        Response<Warehouse> response = warehouseReadService.findById(warehouseId);
        if (!response.isSuccess()){
            log.error("find warehouse by id :{} failed",warehouseId);
            throw new ServiceException("find.warehouse.failed");
        }
        return response.getResult().getCode();
    }

}
