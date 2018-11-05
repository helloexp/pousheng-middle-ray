/**
 * Copyright (C), 2012-2018, XXX有限公司
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.vip.SyncVIPLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 〈yyedi回传退货信息业务处理〉
 * <p>
 * Author: xiehong (168479)
 * Date: 2018/5/31 上午11:41
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.YYEDI_SYNC_REFUND_RESULT)
@Service
@Slf4j
public class YyediSyncRefundService implements CompensateBizService {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private SyncVIPLogic syncVIPLogic;
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("yyedi sync refund service start ....,poushengCompensateBiz is {}",poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("YyediSyncRefundService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("YyediSyncRefundService.doProcess context is null");
            return;
        }
        List<Refund> refunds = JsonMapper.nonEmptyMapper().fromJson(context, JsonMapper.nonEmptyMapper().createCollectionType(List.class, Refund.class));
        if (CollectionUtils.isEmpty(refunds)) {
            log.warn("YyediSyncRefundService.doProcess shipInfos is null");
            return;
        }
        log.info("yyedi sync refund service ,refunds size is {}",refunds.size());
        refunds.stream().forEach(a -> {
            //退货单编码
            String refundCode = a.getRefundCode();
            try {
                //更新扩展信息，主要是yyedi返回的参数
                Map<String,String> extra = a.getExtra();
                Refund update = new Refund();
                update.setId(a.getId());
                update.setExtra(extra);
                Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
                if (!updateExtraRes.isSuccess()) {
                    log.error("update rMatrixRequestHeadefund(refundCode:{}) fail,error:{}", a.getRefundCode(),updateExtraRes.getError());
                }
                this.refundBiz(a.getId());

            } catch (Exception e) {
                log.error("YyediSyncRefundService. forEach refunds ({}) is error: {}", refundCode, Throwables.getStackTraceAsString(e));
            }

        });


    }


    private void refundBiz(Long refundId) {
        //更新扩展信息

        Refund refund = refundReadLogic.findRefundById(refundId);
        Map<String, String> extra = refund.getExtra();
        //店发发货单对应的拒收单不能同步恒康生成pos单
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        //仓发拒收单直接同步订单恒康就好了
        if (Objects.equals(shipment.getShipWay(), 2) && Objects.equals(refund.getRefundType(), MiddleRefundType.REJECT_GOODS.value())) {
            Response<Boolean> r = syncRefundPosLogic.syncSaleRefuseToHK(refund);
            if (!r.isSuccess()) {
                Map<String, Object> param1 = Maps.newHashMap();
                param1.put("refundId", refund.getId());
                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_SALE_REFUSE_TO_HK, r.getError());
            }
            return;
        }
        //同步pos单到恒康
        //判断pos单是否需要同步恒康,如果退货仓数量全是0
        String itemInfo = extra.get(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO);

        List<YYEdiRefundConfirmItem> items = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YYEdiRefundConfirmItem.class));
        if (validateYYConfirmedItems(items)) {
            //生成售后单同步恒康生成pos的任务
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setBizId(String.valueOf(refund.getId()));
            biz.setBizType(PoushengCompensateBizType.SYNC_AFTERSALE_POS_TO_HK.name());
            biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
        }

        this.partRefundDoneProcess(refund, shipment, items);


        //vip 需要通知vip
        ShopOrder shopOrder= orderReadLogic.findShopOrderByCode(refund.getReleOrderCode());
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIP.getValue())) {
            Response<Boolean> response = syncVIPLogic.confirmReturnResult(refund);
            if (!response.isSuccess()) {
                log.error("fail to notice vip refund order  (id:{})  ", refund.getId());
                Map<String, Object> param1 = Maps.newHashMap();
                param1.put("refundId", shipment.getId());
                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_REFUND_TO_VIP, response.getError());
                return;
            }
        }

        //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
        refundWriteLogic.getThirdRefundResult(refund);







    }

    /**
     * 部分退货业务处理
     * @param refund
     * @param shipment
     * @param items
     */
    private void partRefundDoneProcess(Refund refund, Shipment shipment, List<YYEdiRefundConfirmItem> items) {
        //将实际入库数量更新为发货单的refundQuantity
        //实际退货sku-quantity集合
        Map<String,String> refundConfirmItemAndQuantity = items.stream().
                filter(Objects::nonNull).collect(Collectors.toMap(YYEdiRefundConfirmItem::getItemCode,YYEdiRefundConfirmItem::getQuantity));
        //售后申请sku-quantity的集合
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Map<String,Integer> refundApplyItemAndQuantity = refundItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(RefundItem::getSkuCode,RefundItem::getApplyQuantity));
        //校准后发货单售后实际申请数量=当前发货单售后申请数量-(退货单申请数量-售后实际入库数量)
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        for (ShipmentItem shipmentItem:shipmentItems){
            if (refundConfirmItemAndQuantity.containsKey(shipmentItem.getSkuCode())){
                shipmentItem.setRefundQuantity(shipmentItem.getRefundQuantity()-
                        (refundApplyItemAndQuantity.get(shipmentItem.getSkuCode())-
                                Integer.valueOf(refundConfirmItemAndQuantity.get(shipmentItem.getSkuCode()))));
                shipmentItem.setShipmentId(shipment.getId());
            }
        }

        shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);

        //判断申请数量与实际入库数量是否一致
        int count = 0;
        for (RefundItem refundItem:refundItems){
            if (refundConfirmItemAndQuantity.containsKey(refundItem.getSkuCode())){
                //判断申请数量是否一致
                if (!Objects.equals(Integer.valueOf(refundConfirmItemAndQuantity.get(refundItem.getSkuCode())),refundItem.getApplyQuantity())){
                    log.warn("refund item apply quantity not equals confirmed quantity,refundId {},skuCode {},applyQuantity{},confirmedQuantity {}",
                            refund.getId(),refundItem.getSkuCode(),refundItem.getApplyQuantity(),refundConfirmItemAndQuantity.get(refundItem.getSkuCode()));
                    count++;
                }
            }
        }

        if (count>0){
            //refundChangeItemInfo里面参数alreadyHandleNumber=0，状态变更为部分退货完成待确认发货
            refundWriteLogic.updateStatus(refund, MiddleOrderEvent.AFTER_SALE_CHANGE_RE_CREATE_SHIPMENT.toOrderOperation());
        }
    }

    private boolean validateYYConfirmedItems(List<YYEdiRefundConfirmItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        } else {
            int count = 0;
            for (YYEdiRefundConfirmItem item : items) {
                if (Objects.equals(item.getQuantity(), "0")) {
                    count++;
                }
            }
            if (count == items.size()) {
                return false;
            } else {
                return true;
            }
        }
    }
}
