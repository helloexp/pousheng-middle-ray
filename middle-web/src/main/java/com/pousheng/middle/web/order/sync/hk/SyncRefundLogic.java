package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.hksyc.component.SycHkRefundOrderApi;
import com.pousheng.middle.hksyc.dto.HkResponseHead;
import com.pousheng.middle.hksyc.dto.trade.*;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.HkRefundType;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 同步恒康逆向订单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundLogic {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycHkRefundOrderApi sycHkRefundOrderApi;
    @Autowired
    private SycHkOrderCancelApi sycHkOrderCancelApi;
    @Autowired
    private WarehouseReadService warehouseReadService;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步恒康退货单
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundToHk(Refund refund) {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        try {
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            //要根据不同步的售后单类型来决定同步成功或失败的状态
            String reesponse = sycHkRefundOrderApi.doSyncRefundOrder(this.makeSyncHkRefund(refund), this.makeSycHkRefundItemList(refund));
            SycRefundResponse sycRefundResponse = this.makeSycRefundResponse(reesponse);
            //获取响应头
            HkResponseHead head = sycRefundResponse.getHead();
            if (Objects.equals(head.getCode(), 0)) {
                SycHkRefundResponseBody refundBody = sycRefundResponse.getRefundBody();
                //同步调用成功后，更新售后单的状态，及冗余恒康售后单号
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(newStatusRefund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                Refund update = new Refund();
                update.setId(refund.getId());
                update.setOutId(refundBody.getErpOrderNo());
                return refundWriteLogic.update(update);
            } else {
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                return Response.fail("sync.hk.refund.fail");
            }
        } catch (Exception e) {
            log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), e.getMessage());
            Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            return Response.fail("sync.hk.refund.fail");
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
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }

    }


    /**
     * 同步恒康退货单取消
     *
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncRefundCancelToHk(Refund refund) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            String response = sycHkOrderCancelApi.doCancelOrder(String.valueOf(refund.getShopId()), refund.getId(), 1);
            HkResponseHead head = this.makeHkResponseHead(response);
            if (Objects.equals(head.getCode(), 0)) {
                //同步调用成功后，更新售后单的状态
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            } else {
                //同步调用成功后，更新售后单的状态
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                return Response.fail("sync.hk.cancel.refund.failed");
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), e.getMessage());
            return Response.fail("sync.hk.refund.fail");
        }
    }

    /**
     * 组装售后单同步恒康参数1
     *
     * @param refund
     * @return
     */
    private SycHkRefund makeSyncHkRefund(Refund refund) {
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentById(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        SycHkRefund sycHkRefund = new SycHkRefund();
        //中台退换货单号
        sycHkRefund.setRefundNo(String.valueOf(refund.getId()));
        //中台原主订单号--发货单号
        sycHkRefund.setOrderNo(String.valueOf(refundExtra.getShipmentId()));
        //中台店铺id
        sycHkRefund.setShopId(String.valueOf(shipmentExtra.getErpOrderShopCode()));
        sycHkRefund.setStockId(String.valueOf(refundExtra.getWarehouseId()));
        sycHkRefund.setPerformanceShopId(String.valueOf(refund.getShopId()));
        sycHkRefund.setRefundOrderAmount((int) (refund.getFee()==null?0:refund.getFee() / 100));
        sycHkRefund.setRefundFreight(0);
        //换货是在中台完成,不通知恒康,所以只有退款退货,仅退款两项
        //中台状态1:售后退款,2:退货退款,恒康状态0:退货退款,1:仅退款
        sycHkRefund.setType(String.valueOf(this.getHkRefundType(refund).value()));
        //默认状态为待仓库接收
        sycHkRefund.setStatus(String.valueOf(4));
        //售后单创建时间
        sycHkRefund.setCreatedDate(formatter.print(refund.getCreatedAt().getTime()));
        sycHkRefund.setTotalRefund((int) (refund.getFee()==null?0:refund.getFee() / 100));
        //寄回物流单号
        sycHkRefund.setLogisticsCode(refundExtra.getShipmentSerialNo());
        //寄回物流公司代码
        sycHkRefund.setLogisticsCompany(refundExtra.getShipmentCorpCode());

        sycHkRefund.setMemo(refund.getBuyerNote());
        return sycHkRefund;
    }

    /**
     * 组装售后单同步恒康参数2
     *
     * @param refund
     * @return
     */
    private List<SycHkRefundItem> makeSycHkRefundItemList(Refund refund) {
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<SycHkRefundItem> items = Lists.newArrayList();
        for (RefundItem refundItem : refundItems) {
            SycHkRefundItem item = new SycHkRefundItem();
            //中台退换货子单号,用refunId与skuId拼接
            item.setRefundSubNo(refund.getId() + "-" + refundItem.getSkuOrderId());
            //原销售来源子单号
            item.setOrderSubNo(refundExtra.getShipmentId() + "-" + refundItem.getSkuOrderId());
            //恒康商品条码
            item.setBarCode(refundItem.getOutSkuCode());
            //商品数量
            item.setItemNum(refundItem.getApplyQuantity());
            //换货原因,可不填
            item.setReason(refund.getBuyerNote());
            //商品价格
            item.setSalePrice(refundItem.getSkuPrice()==null?0:refundItem.getSkuPrice() / 100);
            //商品总净价
            item.setRefundAmount(refundItem.getCleanFee()==null?0:refundItem.getCleanFee() / 100);
            //商品名称
            item.setItemName(refundItem.getSkuName());
            items.add(item);
        }

        return items;
    }

    /**
     * 组装取消恒康返回参数
     *
     * @param response
     * @return
     */
    public HkResponseHead makeHkResponseHead(String response) throws IOException {
        HkResponseHead responseHead = new HkResponseHead();

        Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);
        if (CollectionUtils.isEmpty(responnseMap)) {
            log.error("sync hk and refundResponseMap is null");
            throw new ServiceException("refund.responseMap.is.null");
        }
        if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
            log.error("refund hk shipmentResponseBody not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
            throw new JsonResponseException("refund.responseMap.head.is.null");
        }
        HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);

        return responseHead;
    }

    /**
     * 组装同步恒康售后单返回参数
     *
     * @param response
     * @return
     */
    public SycRefundResponse makeSycRefundResponse(String response) throws IOException {
        SycRefundResponse sycRefundResponse = new SycRefundResponse();

        Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);

        if (CollectionUtils.isEmpty(responnseMap)) {
            log.error("sync cancel hk and refundResponseMap is null");
            throw new ServiceException("refund.responseMap.is.null");
        }
        if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
            log.error("refundResponseMap not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
            throw new ServiceException("refund.responseMap.head.is.null");
        }
        //获取响应头
        HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);
        sycRefundResponse.setHead(head);
        //如果存在响应body,则返回响应body
        if (responnseMap.containsKey(TradeConstants.SYNC_HK_REFUND_BODY)) {
            SycHkRefundResponseBody refundBody = mapper.fromJson(responnseMap.get(TradeConstants.SYNC_HK_REFUND_BODY), SycHkRefundResponseBody.class);
            sycRefundResponse.setRefundBody(refundBody);
        }
        return sycRefundResponse;
    }

    /**
     * 将中台售后类型映射为恒康的售后类型
     *
     * @param refund
     * @return
     */
    private HkRefundType getHkRefundType(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        switch (middleRefundType) {
            case AFTER_SALES_REFUND:
                return HkRefundType.HK_AFTER_SALES_REFUND;
            case ON_SALES_REFUND:
                return HkRefundType.HK_AFTER_SALES_REFUND;
            case AFTER_SALES_RETURN:
                return HkRefundType.HK_AFTER_SALES_RETURN;
            case AFTER_SALES_CHANGE:
                return HkRefundType.HK_AFTER_SALES_RETURN;
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
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
