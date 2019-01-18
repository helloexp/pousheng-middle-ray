package com.pousheng.middle.web.order.sync.yjerp;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.yyedisyc.component.SycYYEdiRefundOrderApi;
import com.pousheng.middle.yyedisyc.dto.YJErpResponse;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpRefundInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpRefundProductInfo;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;
import java.util.List;
import java.util.Objects;

/**
 * @Description: 同步云聚ERP退货
 * @author: yjc
 * @date: 2018/7/31下午7:49
 */
@Slf4j
@Component
public class SyncYJErpReturnLogic {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycYYEdiRefundOrderApi sycYYEdiRefundOrderApi;
    @Autowired
    private OrderReadLogic orderReadLogic;

    /**
     * 订单退货同步云聚ERP
     *
     * @param refund 退货单
     * @return 同步结果 result 为云聚ERP的退货单编号
     */
    public Response<Boolean> syncRefundToYJErp(Refund refund) {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        try {
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("sync yj erp refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
            refund.setStatus(targetStatus);
            List<YJErpRefundInfo> list = getSycYJErpRefundInfo(refund);
            String response = sycYYEdiRefundOrderApi.doSyncYJErpRefundOrder(list);
            JSONObject responseObj = JSONObject.parseObject(response);
            if (Objects.equals(responseObj.get("error"), 0)) {
                //同步调用成功后，更新售后单的状态，及冗余恒康售后单号
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            }
            else {
                //更新同步状态
                updateRefundSyncFail(refund);
                log.error("订单派发中心返回信息:{}", response);
                return Response.fail("订单派发中心返回信息:" + responseObj.getString("error_info"));
            }
        } catch (Exception e) {
            log.error("sync yj erp refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
            //更新同步状态
            updateRefundSyncFail(refund);
            return Response.fail("sync.yjerp.refund.fail");
        }

        return Response.ok(Boolean.TRUE);
    }


    public List<YJErpRefundInfo> getSycYJErpRefundInfo(Refund refund) {
        List<YJErpRefundInfo> list = Lists.newArrayList();
        list.add(convertYJErpRefundInfo(refund));
        return list;
    }


    public YJErpRefundInfo convertYJErpRefundInfo(Refund refund) {
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        YJErpRefundInfo yjErpRefundInfo = new YJErpRefundInfo();
        // 退货快递单号
        yjErpRefundInfo.setExpress_num(shipmentExtra.getShipmentSerialNo());
        // 中台退货单号
        yjErpRefundInfo.setMg_exchange_sn(refund.getRefundCode());
        // 发货的库房的库房单号
        yjErpRefundInfo.setWarehouse_code(shipmentExtra.getWarehouseOutCode());
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        // 订单号 云聚单号
        yjErpRefundInfo.setOrder_sn(shipmentExtra.getOutShipmentId());
        // 退货商品信息
        List<YJErpRefundProductInfo> list = getYJErpRefundProductInfo(refund);
        yjErpRefundInfo.setProduct_info(list);
        return yjErpRefundInfo;
    }


    public List<YJErpRefundProductInfo> getYJErpRefundProductInfo(Refund refund) {
        List<YJErpRefundProductInfo> list = Lists.newArrayList();
        List<RefundItem> refundItemList = refundReadLogic.findRefundItems(refund);
        refundItemList.forEach(item -> {
            YJErpRefundProductInfo yjErpRefundProductInfo = new YJErpRefundProductInfo();
            yjErpRefundProductInfo.setBar_code(item.getSkuCode());
            yjErpRefundProductInfo.setNum(item.getApplyQuantity());
            // 外观包装良好
            yjErpRefundProductInfo.setAppearance(1);
            // TODO 默认客户拒收 拒收单类型
            yjErpRefundProductInfo.setExchange_reason_id(9);
            // 包装完好
            yjErpRefundProductInfo.setPackaging(2);
            // 非质量问题
            yjErpRefundProductInfo.setProblem_type(0);
            list.add(yjErpRefundProductInfo);
        });
        return list;
    }

    private void updateRefundSyncFail(Refund refund){
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
            case AFTER_SALES_REFUND:
                return MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation();
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.SYNC_CHANGE_SUCCESS.toOrderOperation();
            case REJECT_GOODS:
                return MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation();
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }

    }
}
