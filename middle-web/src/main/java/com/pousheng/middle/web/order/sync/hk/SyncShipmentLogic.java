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
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
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
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

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

            Flow flow = flowPicker.pickShipments();
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);//塞入最新的状态
            List<SycHkShipmentOrderDto> list = this.makeShipmentOrderDtoList(shipment);
            SycErpShipmentOrderResponse response  = JsonMapper.nonDefaultMapper().fromJson(sycHkShipmentOrderApi.doSyncShipmentOrder(list,shipment.getShipmentCode()),SycErpShipmentOrderResponse.class);
            Integer errorCode = response.getErrorCode();
            String description = response.getDescription();
            //解析返回结果
            if (Objects.equals(errorCode,200)) {
                //更新发货单的状态
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_ACCEPT_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            } else {
                log.error("sync hk fail, return code :{},return message:{}",errorCode,description);
                //更新状态为同步失败
                updateShipmetSyncFail(shipment);
                return Response.fail("恒康返回信息:"+description);
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            //更新状态为同步失败
            updateShipmetSyncFail(shipment);
            return Response.fail("sync.hk.shipment.fail");
        }

    }


    private void updateShipmetSyncFail(Shipment shipment){
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_ACCEPT_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }


    private void updateShipmetSyncCancelFail(Shipment shipment){
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

    private void updateShipmetDoneToHkFail(Shipment shipment,OrderOperation syncOrderOperation){
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

    /**
     * 同步发货单取消到恒康
     * @param shipment 发货单
     * @param operationType 0 取消 1 删除 2 收货状态更新
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentCancelToHk(Shipment shipment,Integer operationType) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickShipments();
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);//塞入最新的状态

            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            String response = sycHkOrderCancelApi.doCancelOrder(shipmentExtra.getErpOrderShopCode(), shipment.getShipmentCode(),operationType,0);
            SycShipmentOrderResponse syncShipmentOrderResponse = JsonMapper.nonEmptyMapper().fromJson(response,SycShipmentOrderResponse.class);
            HkResponseHead head = syncShipmentOrderResponse.getHead();
            if (Objects.equals(head.getCode(), "0")) {
                OrderOperation operation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateStatus = shipmentWiteLogic.updateStatus(shipment, operation);
                if (!updateStatus.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                    return Response.fail(updateStatusRes.getError());
                }
            } else {
                //更新状态取消失败
                updateShipmetSyncCancelFail(shipment);
                return Response.fail("恒康返回信息:"+head.getMessage());
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e1) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e1.getMessage());
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail("sync.hk.cancel.shipment.failed");
        }
    }



    /**
     * 自动同步发货单收货信息到恒康
     * @param shipment 发货单
     * @param operationType 0 取消 1 删除 2 收货状态更新
     * @param syncOrderOperation 同步失败的动作(手动和自动略有不同)
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentDoneToHk(Shipment shipment,Integer operationType,OrderOperation syncOrderOperation) {
        try {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            String response = sycHkOrderCancelApi.doCancelOrder(shipmentExtra.getErpOrderShopCode(), shipment.getShipmentCode(),operationType,0);
            SycShipmentOrderResponse syncShipmentOrderResponse = JsonMapper.nonEmptyMapper().fromJson(response,SycShipmentOrderResponse.class);
            HkResponseHead head = syncShipmentOrderResponse.getHead();
            if (Objects.equals(head.getCode(), "0")) {
                OrderOperation operation = MiddleOrderEvent.HK_CONFIRMD_SUCCESS.toOrderOperation();
                Response<Boolean> updateStatus = shipmentWiteLogic.updateStatus(shipment, operation);
                if (!updateStatus.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                    return Response.fail(updateStatus.getError());
                }
            } else {
                //更新状态取消失败
                updateShipmetDoneToHkFail(shipment,MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
                return Response.fail("恒康返回信息:"+head.getMessage());
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e1) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e1.getMessage());
            //更新状态取消失败
            updateShipmetDoneToHkFail(shipment,syncOrderOperation);
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            //更新状态取消失败
            updateShipmetDoneToHkFail(shipment,syncOrderOperation);
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
    public SycHkShipmentOrder getSycHkShipmentOrder(Shipment shipment, ShipmentDetail shipmentDetail) {
        SycHkShipmentOrder tradeOrder = new SycHkShipmentOrder();
        //中台主订单-发货单id
        tradeOrder.setOrderNo(shipment.getShipmentCode());
        //会员账号昵称
        tradeOrder.setBuyerNick(shipmentDetail.getReceiverInfo().getReceiveUserName());
        //订单总金额
        tradeOrder.setOrderMon(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentTotalFee()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN).toString());
        //订单总运费
        tradeOrder.setFeeMon(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentShipFee()-shipmentDetail.getShipmentExtra().getShipmentShipDiscountFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
        //买家应付金额=订单总金额+运费
        tradeOrder.setRealMon(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentTotalPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
        //买家留言
        tradeOrder.setBuyerRemark(shipmentDetail.getShopOrder().getBuyerNote());
        //第三方支付流水号-可以不传
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        OpenClientPaymentInfo paymentInfo = orderReadLogic.getOpenClientPaymentInfo(shopOrder);
        tradeOrder.setPaymentSerialNo(paymentInfo!=null?paymentInfo.getPaySerialNo():"");
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
        Invoice invoice = shipmentDetail.getInvoices()!=null&&shipmentDetail.getInvoices().size()>0?shipmentDetail.getInvoices().get(0):null;
        Map<String,String> invoiceDetail = invoice!=null?invoice.getDetail():null;
        tradeOrder.setInvoice(invoice!=null?invoice.getTitle():"");
        tradeOrder.setTaxNo(invoiceDetail!=null&&invoiceDetail.get("taxRegisterNo")!=null?invoiceDetail.get("taxRegisterNo"):"");

        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        //下单店铺编码
        tradeOrder.setShopId(shipmentExtra.getErpOrderShopCode());
        //todo 下单店名称
        tradeOrder.setShopName("斯凯奇");
        tradeOrder.setOuterOrderNo(shopOrder.getOutId());

        //绩效店铺编码
        tradeOrder.setPerformanceShopId(shipmentExtra.getErpPerformanceShopCode());
        Response<Warehouse> response = warehouseReadService.findById(shipmentExtra.getWarehouseId());
        if (!response.isSuccess()){
            log.error("find warehouse by id :{} failed,  cause:{}",shipmentExtra.getWarehouseId(),response.getError());
            throw new ServiceException(response.getError());
        }
        Warehouse warehouse = response.getResult();
        tradeOrder.setStockId(warehouse.getInnerCode());
        //京东快递,自选快递
        tradeOrder.setVendCustCode(shipmentExtra.getVendCustID());
        //订单来源
        tradeOrder.setOnlineType(String.valueOf(this.getHkOnlinePay(shopOrder).getValue()));

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
            item.setOrderNo(shipment.getShipmentCode());
            //(恒康:中台子订单号),这里拼接了发货单id与skuCode
            item.setOrderSubNo(String.valueOf(shipment.getId()) + "-" + String.valueOf(shipmentItem.getSkuCode()));
            //中台skuCode
            item.setBarcode(shipmentItem.getSkuCode());
            //购买数量--对应中台发货单sku发货数量
            item.setNum(shipmentItem.getQuantity());
            //优惠金额--中台折扣/10
            item.setPreferentialMon(new BigDecimal(shipmentItem.getSkuDiscount()==null?0:shipmentItem.getSkuDiscount()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //销售单价(减去所有的优惠(优惠需要按比例计算))
            item.setSalePrice(new BigDecimal(shipmentItem.getCleanPrice()==null?0:shipmentItem.getCleanPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //总价(销售价格*数量)
            item.setTotalPrice(new BigDecimal(shipmentItem.getCleanFee()==null?0:shipmentItem.getCleanFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //赠品(1),非赠品(2)默认填写非赠品
            //电商订单号
            item.setOnlineOrderNo(shipmentDetail.getShopOrder().getOutId());
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
        Map<String,String> responnseMap = mapper.fromJson(response, mapper.createCollectionType(HashMap.class, String.class, String.class));
        //Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);
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
        if (shipmentDetail.getInvoices()!=null&&shipmentDetail.getInvoices().size()>0){
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
        }else{
            return HKInvoiceType.PLAIN_INVOICE;
        }

    }
    /**
     * 获取恒康Code
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
    /**
     * 中台支付类型映射为恒康支付类型
     *
     * @param shopOrder 店铺订单
     * @return
     */
    public HkOnlineType getHkOnlinePay(ShopOrder shopOrder) {
        MiddleChannel middleChannel = MiddleChannel.from(shopOrder.getOutFrom());
        if (Arguments.isNull(middleChannel)) {
            log.error("shopOrder (id:{})invalid", shopOrder.getId());
            throw new ServiceException("shoporder.channel.invalid");
        }
        switch (middleChannel) {
            case TAOBAO:
                return HkOnlineType.TAOBAO;
            case JD:
                return HkOnlineType.JINGDONG;
            case SUNING:
                return HkOnlineType.SUNING;
            case OFFICIAL:
                return HkOnlineType.OFFICIAL;
            case FENQILE:
                return HkOnlineType.FENQILE;
            default:
                log.error("shopOrder (id:{}) invalid", shopOrder.getId());
                throw new ServiceException("shoporder.channel.invalid");
        }
    }

}
