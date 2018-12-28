package com.pousheng.middle.web.order.sync.yyedi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.yyedisyc.component.SycYYEdiRefundCancelApi;
import com.pousheng.middle.yyedisyc.component.SycYYEdiRefundOrderApi;
import com.pousheng.middle.yyedisyc.dto.YYEdiResponse;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnItem;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 同步恒康逆向订单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncYYEdiReturnLogic {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private SycYYEdiRefundOrderApi sycYYEdiRefundOrderApi;
    @Autowired
    private SycYYEdiRefundCancelApi sycYYEdiRefundCancelApi;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将售后退货退款，换货单同步到订单派发中心
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundToYYEdi(Refund refund) {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        try {
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
            refund.setStatus(targetStatus);

            //要根据不同步的售后单类型来决定同步成功或失败的状态
            YYEdiReturnInfo returnInfo =   this.makeSyncYYEdiRefund(refund);
            String response = sycYYEdiRefundOrderApi.doSyncRefundOrder(returnInfo);
            YYEdiResponse yyEdiResponse =JsonMapper.nonEmptyMapper().fromJson(response,YYEdiResponse.class);
            if (Objects.equals(yyEdiResponse.getCode(),TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)){
                //同步调用成功后，更新售后单的状态，及冗余恒康售后单号
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            }else{
                //更新同步状态
                updateRefundSyncFial(refund);
                return Response.fail("订单派发中心返回信息:"+yyEdiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("sync yyedi refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
            //更新同步状态
            updateRefundSyncFial(refund);
            return Response.fail("sync.yyedi.refund.fail");
        }

        return Response.ok(Boolean.TRUE);
    }


    private void updateRefundSyncFial(Refund refund){
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
        }

    }

    //获取同步成功事件
    private OrderOperation getSyncSuccessOperation(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        if (Arguments.isNull(middleRefundType)) {
            log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
            throw new JsonResponseException("refund.type.invalid");
        }

        switch (middleRefundType) {
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation();
            case AFTER_SALES_REFUND:
                return MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation();
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.SYNC_CHANGE_SUCCESS.toOrderOperation();
            case REJECT_GOODS:
                return MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }

    }



    /**
     * 组装售后单同步恒康参数1
     *
     * @param refund
     * @return
     */
    public YYEdiReturnInfo makeSyncYYEdiRefund(Refund refund) {
        YYEdiReturnInfo refundInfo = new YYEdiReturnInfo();
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        WarehouseDTO warehouse = null;
        if (refundExtra.getWarehouseId()!=null){
            Response<WarehouseDTO> response = warehouseClient.findById(refundExtra.getWarehouseId());
            if (!response.isSuccess()){
                log.error("find warehouse by id :{} failed,  cause:{}",shipmentExtra.getWarehouseId(),response.getError());
                throw new ServiceException(response.getError());
            }
            warehouse = response.getResult();
            //公司码
            refundInfo.setCompanycode(warehouse.getCompanyCode());
            //仓库
            refundInfo.setStockcode(warehouse.getOutCode());
        }
        //退货单号
        refundInfo.setBillno(String.valueOf(refund.getRefundCode()));
        //来源单号
        Shipment sourceShipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        refundInfo.setSourcebillno(sourceShipment.getShipmentCode());
        //网店交易单号
        refundInfo.setShopbillno(shopOrder.getOutId());
        //单据类型
        refundInfo.setBilltype(TradeConstants.YYEDI_BILL_TYPE_RETURN);
        //店铺内码
        refundInfo.setShopcode(shipmentExtra.getErpPerformanceShopOutCode());
        //店铺名称
        refundInfo.setShopname(shipmentExtra.getErpPerformanceShopName());
        //买家用户名
        refundInfo.setBcmembername(shopOrder.getBuyerName());
        //客户供应商快递公司内码
        refundInfo.setCustomercode("");
        refundInfo.setCustomername("");
        refundInfo.setExpressbillno(refundExtra.getShipmentSerialNo());
        refundInfo.setIsrefundinvoice(0);
        //1.退货，0.换货
        refundInfo.setRefundchangetype(refund.getRefundType()==2?1:0);
        //退款金额
        refundInfo.setCollectionamount(new BigDecimal(refund.getFee()==null?0:refund.getFee()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN));
        //邮费
        refundInfo.setExpressamount(new BigDecimal(0.00));
        //中台没有运费到付,所以填0
        refundInfo.setFreightpay(0);
        //寄件人信息
        ReceiverInfo receiverInfo = refundExtra.getReceiverInfo();
        if (!Objects.isNull(receiverInfo)){
            refundInfo.setSendcontact(receiverInfo.getReceiveUserName());
            //寄件人电话
            refundInfo.setSendcontacttel(receiverInfo.getMobile());
            //寄件省
            refundInfo.setSendprovince(receiverInfo.getProvince());
            //寄件市
            refundInfo.setSendcity(receiverInfo.getCity());
            //寄件区
            refundInfo.setSendarea(receiverInfo.getRegion());
            //寄件地址
            refundInfo.setSendaddress(receiverInfo.getDetail());
            //寄件邮编
            refundInfo.setZipcode(receiverInfo.getPostcode());
        }else{
            refundInfo.setSendcontact("");
            //寄件人电话
            refundInfo.setSendcontacttel("");
            //寄件省
            refundInfo.setSendprovince("");
            //寄件市
            refundInfo.setSendcity("");
            //寄件区
            refundInfo.setSendarea("");
            //寄件地址
            refundInfo.setSendaddress("");
            //寄件邮编
            refundInfo.setZipcode("");
        }
        //最近修改时间
        //refundInfo.setERPModifyTime(formatter.print(refund.getCreatedAt().getTime()));
        //明细记录
        List<YYEdiReturnItem> items = this.makeSycYYEdiRefundItemList(refund,warehouse,shopOrder);
        refundInfo.setItems(items);
        int quantity = 0;
        for (YYEdiReturnItem returnItem:items){
            quantity =quantity+returnItem.getExpectqty();
        }
        //预期数量
        refundInfo.setExpectqty(quantity);
        //总行数
        refundInfo.setTdq(items.size());
        //yyedi订单号
        if (!StringUtils.isEmpty(shipmentExtra.getOutShipmentId())){
            refundInfo.setEdibillno(shipmentExtra.getOutShipmentId());
        }else{
            refundInfo.setEdibillno(String.valueOf(shipment.getId()));

        }
        return refundInfo;
    }

    /**
     * 组装售后单同步恒康参数2
     *
     * @param refund
     * @return
     */
    private List<YYEdiReturnItem> makeSycYYEdiRefundItemList(Refund refund,WarehouseDTO warehouse,ShopOrder shopOrder) {
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        List<YYEdiReturnItem> items = Lists.newArrayList();
        int count= 0;
        for (RefundItem refundItem : refundItems) {
            YYEdiReturnItem item = new YYEdiReturnItem();
             //行号
            item.setRowno(count);
            //公司内码
            item.setCompanycode(warehouse.getCompanyCode());
            //ERP单号
            item.setBillno(String.valueOf(refund.getRefundCode()));
            //条码
            item.setSku(refundItem.getSkuCode());
            //货号
            Response<List<SkuTemplate>> rS = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(refundItem.getSkuCode()));
            if (!rS.isSuccess()){
                throw new ServiceException("find.sku.template.failed");
            }
            List<SkuTemplate> skuTemplates = rS.getResult();
            Optional<SkuTemplate> skuTemplateOptional = skuTemplates.stream().filter(skuTemplate->!Objects.equals(skuTemplate.getStatus(),-3)).findAny();
            if (!skuTemplateOptional.isPresent()){
                throw new ServiceException("sku.template.may.be.canceled");
            }
            SkuTemplate skuTemplate = skuTemplateOptional.get();
            Map<String,String> extraMaps = skuTemplate.getExtra();
            String materialCode = extraMaps.get(TradeConstants.HK_MATRIAL_CODE);
            //货号
            item.setMaterialcode(materialCode);
            //尺码名称
            String sizeName = "";
            List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            for (SkuAttribute skuAttribute:skuAttributes){
                if (Objects.equals(skuAttribute.getAttrKey(),"尺码")){
                    sizeName = skuAttribute.getAttrVal();
                }
            }
            item.setSizename(sizeName);
            //预计数量
            item.setExpectqty(refundItem.getApplyQuantity());
            //网店交易单号
            item.setShopbillno(shopOrder.getOutId());
            //结算价(单价)
            item.setBalaprice(new BigDecimal(refundItem.getSkuPrice()==null?0:refundItem.getSkuPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //结算金额
            item.setPayamount(new BigDecimal(refundItem.getFee()==null?0:refundItem.getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //零售价-吊牌价
            Map<String,Integer> extraPrice = skuTemplate.getExtraPrice();
            int originPrice = extraPrice.get("originPrice")==null?0:extraPrice.get("originPrice");
            item.setRetailprice(new BigDecimal(originPrice).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //edi订单号
            item.setEdibillno(shipmentExtra.getOutShipmentId());
            items.add(item);
            count++;
        }

        return items;
    }

    /**
     * 获取恒康
     * @param warehouseId
     * @return
     */
    private String getHkWarehouseCodeById(long warehouseId){
        Response<WarehouseDTO> response = warehouseClient.findById(warehouseId);
        if (!response.isSuccess()){
            log.error("find warehouse by id :{} failed",warehouseId);
            throw new ServiceException("find.warehouse.failed");
        }
        return response.getResult().getWarehouseCode();
    }

    /**
     * 同步恒康退货单取消
     *
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncRefundCancelToYyEdi(Refund refund) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            YYEdiCancelInfo yyEdiCancelInfo = new YYEdiCancelInfo();
            yyEdiCancelInfo.setBillno(refund.getRefundCode());
            yyEdiCancelInfo.setRemark(refund.getBuyerNote());
            String response = sycYYEdiRefundCancelApi.doCancelOrder(yyEdiCancelInfo);
            YYEdiResponse yyEdiResponse = JsonMapper.nonEmptyMapper().fromJson(response,YYEdiResponse.class);
            if (Objects.equals(yyEdiResponse.getCode(),TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)) {
                //同步调用成功后，更新售后单的状态
                Response<Boolean> updateSyncStatusRes = this.upateCancelRefundSuccess(refund, orderOperation, updateStatusRes);
                if (updateSyncStatusRes != null){
                    return updateSyncStatusRes;
                }
            } else if (Objects.equals(yyEdiResponse.getCode(), TradeConstants.YYEDI_RESPONSE_CODE_ING)) {
                //取消中 加个标记位 依然返回取消失败
                Map<String, String> extraMap = refund.getExtra();
                extraMap.put(TradeConstants.WAIT_CANCEL_RESULT, "1");
                Refund update =new Refund();
                update.setExtra(extraMap);
                update.setId(refund.getId());
                refundWriteLogic.update(update);
                return Response.fail("订单派发中心返回信息:" + yyEdiResponse.getMessage());
            } else {
                //同步调用取消失败，更新售后单的状态
                this.updateCancelRefundFailed(refund);
                return Response.fail("订单派发中心返回信息:"+yyEdiResponse.getMessage());
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            //同步调用成功后，更新售后单的状态
            this.updateCancelRefundFailed(refund);
            log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("sync.hk.refund.fail");
        }
    }

    @Nullable
    private Response<Boolean> upateCancelRefundSuccess(Refund refund, OrderOperation orderOperation, Response<Boolean> updateStatusRes) {
        Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
        OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(newStatusRefund, syncSuccessOrderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }
        return Response.ok(Boolean.TRUE);
    }

    @Nullable
    private Response<Boolean> updateCancelRefundFailed(Refund refund) {
        Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
        OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(newStatusRefund, syncSuccessOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }
        return Response.ok(Boolean.TRUE);
    }
}
