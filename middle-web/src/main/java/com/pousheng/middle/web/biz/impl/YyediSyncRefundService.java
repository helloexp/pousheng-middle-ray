/**
 * Copyright (C), 2012-2018, XXX有限公司
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
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
import com.pousheng.middle.web.utils.OutSkuCodeUtil;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.*;
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

    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;

    @Autowired
    private OpenShopCacher openShopCacher;

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
                //更新扩展信息，以及将实际退货数量更新到RefundItem表中
                Refund refund = this.addFinalRefundItems(a);

                Map<String,String> extra = refund.getExtra();
                Refund update = new Refund();
                update.setId(refund.getId());
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
        } else {
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
                compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
            }

            this.partRefundDoneProcess(refund, shipment);

            //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
            refundWriteLogic.getThirdRefundResult(refund);

        }
        //vip 需要通知vip
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(refund.getReleOrderCode());
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
            Response<Boolean> response = syncVIPLogic.confirmReturnResult(refund);
            if (!response.isSuccess()) {
                log.error("fail to notice vip refund order  (id:{})  ", refund.getId());
                Map<String, Object> param1 = Maps.newHashMap();
                param1.put("refundId", refund.getId());
                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_REFUND_TO_VIP, response.getError());
                return;
            }
        }
        //如果是天猫换货单 需要通知天猫
        OpenShop openShop = openShopCacher.findById(refund.getShopId());//根据店铺id查询店铺
        Map<String, String> extraMap = openShop.getExtra();
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())&&Objects.equals(openShop.getChannel(), MiddleChannel.TAOBAO.getValue())&&extraMap.containsKey(TradeConstants.EXCHANGE_PULL) && Objects.equals(extraMap.get(TradeConstants.EXCHANGE_PULL), "Y")) {
            //卖家确认收货，等待卖家发货调用 tmall.exchange.returngoods.agree 更改店 铺换货状态为当前状态
            OpenClientAfterSaleExchangeService afterSaleExchangeService = afterSaleExchangeServiceRegistryCenter.getAfterSaleExchangeService(openShop.getChannel());
            String outerAfterSaleId = refundReadLogic.getOutafterSaleIdTaobao(refund.getOutId());
            Response<Boolean> result = afterSaleExchangeService.sellerConfirmReceipt(openShop.getId(),outerAfterSaleId);
            if (!result.isSuccess()) {
                log.error("fail to notice taobao refund order  (id:{})  ", refund.getId());
                Map<String, Object> param2 = Maps.newHashMap();
                param2.put("refundId", refund.getId());
                autoCompensateLogic.createAutoCompensationTask(param2, TradeConstants.FAIL_REFUND_TO_TMALL, result.getError());
                return;
            }
        }
    }

    /**
     * 部分退货业务处理
     * @param refund
     * @param shipment
     */
    private void partRefundDoneProcess(Refund refund, Shipment shipment) {
        //售后申请sku-quantity的集合
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);

        //将实际入库数量更新为发货单的refundQuantity
        //实际退货sku-quantity集合
        Map<String, Integer> refundApplyQuantityMap = Maps.newHashMap();
        Map<String, Integer> refundConfirmQuantityMap = Maps.newHashMap();

        for (RefundItem refundItem : refundItems) {
            String key = OutSkuCodeUtil.getRefundItemComplexSkuCode(refundItem);
            Integer applyQuantity = MoreObjects.firstNonNull(refundApplyQuantityMap.get(key), 0)
                    + refundItem.getApplyQuantity();
            Integer confirmQuantity = MoreObjects.firstNonNull(refundConfirmQuantityMap.get(key), 0)
                    + MoreObjects.firstNonNull(refundItem.getFinalRefundQuantity(), 0);
            refundApplyQuantityMap.put(key, applyQuantity);
            refundConfirmQuantityMap.put(key, confirmQuantity);
        }

        //校准后发货单售后实际申请数量=当前发货单售后申请数量-(退货单申请数量-售后实际入库数量)
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        for (ShipmentItem shipmentItem:shipmentItems){
            String shipmentComplexSkuCode = OutSkuCodeUtil.getShipmentItemComplexSkuCode(shipmentItem);
            Integer confirmQuantity = refundConfirmQuantityMap.get(shipmentComplexSkuCode);
            if (confirmQuantity != null && confirmQuantity > 0) {
                Integer applyQuantity = refundApplyQuantityMap.get(shipmentComplexSkuCode);
                // 这个逻辑有点看不懂
                shipmentItem.setRefundQuantity(shipmentItem.getRefundQuantity() - (applyQuantity - confirmQuantity));
                shipmentItem.setShipmentId(shipment.getId());
            }
        }
        shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);
        //判断申请数量与实际入库数量是否一致
        int count = 0;
        for (RefundItem refundItem : refundItems) {
            Integer confirmQuantity = refundItem.getFinalRefundQuantity();
            if (confirmQuantity != null) {
                if (!Objects.equals(refundItem.getApplyQuantity(), refundItem.getFinalRefundQuantity())) {
                    log.warn("refund item apply quantity not equals confirmed quantity,refundId {}, applyQuantity{}, confirmedQuantity {}",
                            refund.getId(),refundItem.getApplyQuantity(), confirmQuantity);
                    count++;
                }
            } else {
                //如果有申请记录，因为申请记录不为0，实际退货又没有这个条码，默认申请数量与实际入库数量不一致
                count++;
            }
        }
        if (count>0){
            //refundChangeItemInfo里面参数alreadyHandleNumber=0，状态变更为部分退货完成待确认发货
            refundWriteLogic.updateStatus(refund, MiddleOrderEvent.AFTER_SALE_ECHANGE_PART_DONE_RETURN.toOrderOperation());
        }
    }

    /**
     * 最终退货的数量记录
     * @param refund
     */
    private Refund addFinalRefundItems(Refund refund){
        Map<String, String> extra = refund.getExtra();
        String itemInfo = extra.get(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO);
        if (!StringUtils.isEmpty(itemInfo)){

            List<YYEdiRefundConfirmItem> yyEdiRefundConfirmItems = JsonMapper.nonEmptyMapper().fromJson(itemInfo,
                    JsonMapper.nonEmptyMapper().createCollectionType(List.class, YYEdiRefundConfirmItem.class));

            List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
            setFinalRefundQuantity(refundItems, yyEdiRefundConfirmItems);
            extra.put(TradeConstants.REFUND_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundItems));
            extra.put(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO,
                    JsonMapper.nonEmptyMapper().toJson(yyEdiRefundConfirmItems));
            refund.setExtra(extra);
        }
        return refund;
    }

    /**
     * 先把 yyEdiRefundConfirmItems 按照 skuCode 汇总
     * 然后依次根据申请数量从大到小回写
     *
     * @param refundItems
     * @param yyEdiRefundConfirmItems
     */
    private void setFinalRefundQuantity(List<RefundItem> refundItems,
                                        List<YYEdiRefundConfirmItem> yyEdiRefundConfirmItems) {
        Map<String, Integer> skuCode2Quantities = Maps.newHashMap();
        // 汇总
        for (YYEdiRefundConfirmItem yyEdiRefundConfirmItem : yyEdiRefundConfirmItems) {
            if (Objects.nonNull(yyEdiRefundConfirmItem)) {
                String itemCode = yyEdiRefundConfirmItem.getItemCode();
                Integer quantity = Integer.valueOf(yyEdiRefundConfirmItem.getQuantity());
                if (skuCode2Quantities.containsKey(itemCode)) {
                    quantity += skuCode2Quantities.get(itemCode);
                    skuCode2Quantities.put(itemCode, quantity);
                } else {
                    skuCode2Quantities.put(itemCode, quantity);
                }
            }
        }
        // 按照申请数量降序
        refundItems.sort((o1, o2) -> o2.getApplyQuantity() - o1.getApplyQuantity());
        // 按照降序回写
        for (RefundItem refundItem : refundItems) {
            String key = refundItem.getSkuCode();
            if (skuCode2Quantities.containsKey(key)) {
                Integer remainQuantity = skuCode2Quantities.get(key);
                Integer applyQuantity = refundItem.getApplyQuantity();
                Integer finalQuantity = remainQuantity >= applyQuantity ? applyQuantity : remainQuantity;
                refundItem.setFinalRefundQuantity(finalQuantity);
                skuCode2Quantities.put(key, remainQuantity - finalQuantity);
                refundItem.setFinalRefundQuantity(finalQuantity);
            } else {
                //如果没有返回记录，则说明实际退货数量为0
                refundItem.setFinalRefundQuantity(0);
            }
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
