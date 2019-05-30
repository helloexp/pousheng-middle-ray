package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.utils.SkuCodeUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.aftersale.component.DefaultAfterSaleReceiver;
import io.terminus.open.client.center.job.aftersale.dto.SkuOfRefund;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.dto.OpenClientAfterSaleItem;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.utils.RespUtil;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.pousheng.middle.web.order.JitShipmentController.MAPPER;

/**
 * Created by cp on 7/17/17.
 */
@Component
@Slf4j
public class PsAfterSaleReceiver extends DefaultAfterSaleReceiver {

    private final PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private ExpressCodeReadService expressCodeReadService;
    @Autowired
    private PsAfterSaleReceiverHelper psAfterSaleReceiverHelper;

    @RpcConsumer
    private RefundReadService refundReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    public PsAfterSaleReceiver(PoushengMiddleSpuService middleSpuService) {
        this.middleSpuService = middleSpuService;
    }

    @Override
    protected List<Refund> fillSkuInfo(ShopOrder shopOrder, Refund refund, List<SkuOfRefund> skuOfRefundList) {
        log.info("psAfterSaleReceiver skuCodes is ({})", JsonMapper.nonEmptyMapper().toJson(skuOfRefundList));
        log.info("psAfterSaleReceiver shopOrderId is ({})", shopOrder.getId());

        List<Refund> refundList = Lists.newArrayList();
        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(shopOrder.getId());
        //塞入地址信息
        RefundExtra refundExtra = new RefundExtra();
        refundExtra.setReceiverInfo(receiverInfo);
        //关联单号
        refundExtra.setReleOrderNo(shopOrder.getOrderCode());
        //关联单号类型
        refundExtra.setReleOrderType(1);
        if (Objects.equals(MiddleRefundType.ON_SALES_REFUND.value(), refund.getRefundType())) {
            //借用tradeNo字段来标记售中退款的逆向单是否已处理
            refund.setTradeNo(TradeConstants.REFUND_WAIT_CANCEL);
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        } else {
            //其他类型的售后单添加一个锁标识
            refund.setTradeNo(TradeConstants.AFTER_SALE_EXHCANGE_UN_LOCK);
        }
        //判断售后单对应的是否是天猫订单
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())
            && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
            //如果天猫拉取过来的仅退款的售后单就是success,这个时候中台做一下特殊处理
            if (Objects.equals(refund.getStatus(), MiddleRefundStatus.REFUND.getValue())) {
                //判断此时的订单以及发货单的状态
                refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
                //判断是否是售中退款
                if (orderReadLogic.isOnSaleRefund(shopOrder.getId())) {
                    refund.setRefundType(MiddleRefundType.ON_SALES_REFUND.value());
                    refund.setTradeNo(TradeConstants.REFUND_WAIT_CANCEL);
                }
            }
        }
        //天猫分销售后单
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TFENXIAO.getValue())
                && (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())
                || Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value()))) {
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }

        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())
            && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
            //如果天猫拉取过来的退货退款的售后单就是success,这个时候中台做一下特殊处理
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }
        //苏宁售后近退款单子到中台订单状态做初始化
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.SUNING.getValue())
            && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }
        //当skuOfRefundList的size大于1时，说明是一个发货单发货
        if (skuOfRefundList.size() > 1) {
            refundList = fillRefundItem(shopOrder, refund, skuOfRefundList, refundExtra);
        } else {
            refundList = fillRefundItem(shopOrder, refund, skuOfRefundList.get(0), refundExtra);
        }
        log.info("refundList {} ,size{}", refundList.toString(), refundList.size());
        return refundList;

    }

    /**
     * @param shopOrder
     * @param refund
     * @param skuOfRefundList
     * @param refundExtra
     */
    private List<Refund> fillRefundItem(ShopOrder shopOrder, Refund refund, List<SkuOfRefund> skuOfRefundList,
                                        RefundExtra refundExtra) {
        try {
            List<RefundItem> refundItemList = Lists.newArrayList();
            Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();
            skuOfRefundList.forEach(skuOfRefund ->
            {
                SkuOrder skuOrder;
                if (StringUtils.hasText(skuOfRefund.getChannelSkuId())) {
                    skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndOutSkuId(shopOrder.getId(),
                        skuOfRefund.getChannelSkuId());
                    skuOfRefund.setSkuCode(skuOrder.getSkuCode());
                } else if (StringUtils.hasText(skuOfRefund.getChannelSkuOrderId())) {
                    skuOrder = orderReadLogic.findSkuOrderByShopOrderIfAndIOutSkuOrderId(shopOrder.getId(),
                        skuOfRefund.getChannelSkuOrderId());
                    skuOfRefund.setSkuCode(skuOrder.getSkuCode());
                } else {
                    skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(shopOrder.getId(),
                        skuOfRefund.getSkuCode());
                }
                //查询需要售后的发货单
                //Shipment shipment = this.findShipmentByOrderInfo(shopOrder.getId(), skuOfRefund.getSkuCode(), skuOrder.getQuantity());
                Shipment shipment = this.findShipmentByOrderInfo(shopOrder.getId(), skuOrder);



                if (!Objects.isNull(shipment)) {
                    refundExtra.setShipmentId(shipment.getShipmentCode());
                    //添加售后仓库
                    try {
                        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
                        String warehouseId = orderReadLogic.getOpenShopExtraMapValueByKey(
                            TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID, openShop);
                        String warehouseName = orderReadLogic.getOpenShopExtraMapValueByKey(
                            TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME, openShop);
                        refundExtra.setWarehouseId(Long.valueOf(warehouseId));
                        refundExtra.setWarehouseName(warehouseName);
                        //表明售后单的信息已经全部完善
                        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
                    } catch (ServiceException e) {
                        log.error("find warehouse info failed,caused by {}", Throwables.getStackTraceAsString(e));
                    }
                }

                RefundItem refundItem = new RefundItem();
                refundItem.setSkuCode(skuOrder.getSkuCode());
                refundItem.setSkuOrderId(skuOrder.getId());
                refundItem.setOutSkuCode(skuOrder.getOutSkuId());

                if (!Objects.isNull(shipment)) {
                    List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                    //ShipmentItem shipmentItem = shipmentItems
                    //        .stream().filter(shipmentItem1 ->
                    //                Objects.equals(shipmentItem1.getSkuCode(), skuOfRefund.getSkuCode())).collect(Collectors.toList()).get(0);
                    ShipmentItem shipmentItem = shipmentItems.stream().filter(si -> SkuCodeUtil.compareCombineCode(si, skuOrder))
                            .findFirst().get();

                    if ((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) > 0) {
                        log.warn("this refund item has been applied,refundSkuCode is {}", skuOfRefund.getSkuCode());
                        refund.setStatus(MiddleRefundStatus.DELETED.getValue());
                        refund.setSellerNote("系统：订单商品已产生售后，当前订单不同步ERP");
                        return;
                    }
                    refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
                    refundItem.setSkuPrice(shipmentItem.getSkuPrice());
                    refundItem.setSkuDiscount(shipmentItem.getSkuDiscount());
                    refundItem.setCleanFee(shipmentItem.getCleanFee());
                    refundItem.setCleanPrice(shipmentItem.getCleanPrice());
                    refundItem.setAlreadyHandleNumber(shipmentItem.getQuantity());
                    List<SkuAttribute> attrs = shipmentItem.getAttrs();
                    refundItem.setAttrs(attrs);
                    refundItem.setItemId(shipmentItem.getItemId());
                    refundItem.setApplyQuantity(shipmentItem.getQuantity());
                    refundItem.setSharePlatformDiscount(shipmentItem.getSharePlatformDiscount());
                    //售中退款不需要更新退货数量
                    if (!Objects.equals(refund.getRefundType(), MiddleRefundType.ON_SALES_REFUND.value()) &&
                            !Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
                        //updateShipmentItemRefundQuantity(skuOfRefund.getSkuCode(), shipmentItem.getQuantity(), shipmentItems);
                        updateShipmentItemRefundQuantity(refundItem, shipmentItem.getQuantity(), shipmentItems);

                    }
                    //更新发货单商品中的已退货数量
                    //Map<String, String> shipmentExtraMap = shipment.getExtra();
                    //shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson
                    // (shipmentItems));
                    //如果拉取下来的售后单是已取消，则不需要更新已退货数量
                    if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.CANCELED.getValue())) {
                        //shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
                        //更新发货单明细
                        shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);
                    }
                }

                //获取skuCode
                try {
                    SkuTemplate skuTemplate = this.findSkuTemplateBySkuCode(skuOrder.getSkuCode());
                    refundItem.setAttrs(skuTemplate.getAttrs());
                } catch (Exception e) {
                    log.error("find sku template failed,skuCode is {},caused by {}", skuOrder.getSkuCode(),
                        Throwables.getStackTraceAsString(e));
                }
                refundItem.setSkuName(skuOrder.getItemName());
                refundItemList.add(refundItem);
            });
            extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(refundItemList));
            Map<String, String> tagMap = Maps.newHashMap();
            tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.THIRD.value()));

            refund.setExtra(extraMap);
            refund.setTags(tagMap);
        } catch (Exception e) {
            log.error("create refund find error,shopOrderId is {},caused by {}", shopOrder.getId(),
                Throwables.getStackTraceAsString(e));
        }
        return Lists.newArrayList(refund);
    }

    /**
     * @param shopOrder
     * @param originRefund
     * @param skuOfRefund
     * @param refundExtra
     */
    private List<Refund> fillRefundItem(ShopOrder shopOrder, Refund originRefund, SkuOfRefund skuOfRefund,
                                        RefundExtra refundExtra) {
        List<Refund> refundList = Lists.newArrayList();
        try {
            SkuOrder skuOrder;
            if (StringUtils.hasText(skuOfRefund.getChannelSkuId())) {
                skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndOutSkuId(shopOrder.getId(),
                    skuOfRefund.getChannelSkuId());
                skuOfRefund.setSkuCode(skuOrder.getSkuCode());
            } else if (StringUtils.hasText(skuOfRefund.getChannelSkuOrderId())) {
                skuOrder = orderReadLogic.findSkuOrderByShopOrderIfAndIOutSkuOrderId(shopOrder.getId(), skuOfRefund.getChannelSkuOrderId());

                skuOfRefund.setSkuCode(skuOrder.getSkuCode());
            } else {
                skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(shopOrder.getId(),
                    skuOfRefund.getSkuCode());
            }
            //查询需要售后的发货单
            //List<Shipment> shipments = this.findShipmentByOrderInfo(shopOrder.getId(), skuOfRefund.getSkuCode());
            List<Shipment> shipments = this.findShipmentBySkuOrderInfo(shopOrder.getId(), skuOrder);

            if (!CollectionUtils.isEmpty(shipments)) {
                for (Shipment shipment : shipments) {
                    if (!Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())
                        && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                        && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())) {
                        log.info("shipment is not shipped shipmentCode {} ,status {}", shipment.getShipmentCode(),
                            shipment.getStatus());
                        continue;
                    }
                    List<RefundItem> refundItemList = Lists.newArrayList();
                    Map<String, String> extraMap = originRefund.getExtra() != null ? originRefund.getExtra()
                        : Maps.newHashMap();
                    Refund refund = new Refund();
                    BeanUtils.copyProperties(originRefund, refund);
                    refundExtra.setShipmentId(shipment.getShipmentCode());
                    //添加售后仓库
                    try {
                        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
                        String warehouseId = orderReadLogic.getOpenShopExtraMapValueByKey(
                            TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID, openShop);
                        String warehouseName = orderReadLogic.getOpenShopExtraMapValueByKey(
                            TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME, openShop);
                        refundExtra.setWarehouseId(Long.valueOf(warehouseId));
                        refundExtra.setWarehouseName(warehouseName);
                        //表明售后单的信息已经全部完善
                        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
                    } catch (ServiceException e) {
                        log.error("find warehouse info failed,caused by {}", Throwables.getStackTraceAsString(e));
                    }
                    RefundItem refundItem = new RefundItem();
                    refundItem.setSkuCode(skuOrder.getSkuCode());
                    refundItem.setSkuOrderId(skuOrder.getId());
                    refundItem.setOutSkuCode(skuOrder.getOutSkuId());

                    List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                    //ShipmentItem shipmentItem = shipmentItems
                    //        .stream().filter(shipmentItem1 ->
                    //                Objects.equals(shipmentItem1.getSkuCode(), skuOfRefund.getSkuCode())).collect(Collectors.toList()).get(0);
                    ShipmentItem shipmentItem = shipmentItems.stream().filter(si -> SkuCodeUtil.compareCombineCode(si, refundItem))
                            .findFirst().get();

                    if ((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) > 0) {
                        log.warn("this refund item has been applied,refundSkuCode is {}", skuOfRefund.getSkuCode());
                        refund.setStatus(MiddleRefundStatus.DELETED.getValue());
                        refund.setSellerNote("系统：订单商品已产生售后，当前订单不同步ERP");
                        refundList.add(refund);
                        continue;
                    }
                    refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
                    refundItem.setSkuPrice(shipmentItem.getSkuPrice());
                    refundItem.setSkuDiscount(shipmentItem.getSkuDiscount());
                    refundItem.setCleanFee(shipmentItem.getCleanFee());
                    refundItem.setCleanPrice(shipmentItem.getCleanPrice());
                    refundItem.setAlreadyHandleNumber(shipmentItem.getQuantity());
                    List<SkuAttribute> attrs = shipmentItem.getAttrs();
                    refundItem.setAttrs(attrs);
                    refundItem.setItemId(shipmentItem.getItemId());
                    refundItem.setApplyQuantity(shipmentItem.getQuantity());
                    refundItem.setSharePlatformDiscount(shipmentItem.getSharePlatformDiscount());
                    //售中退款不需要更新退货数量
                    if (!Objects.equals(refund.getRefundType(), MiddleRefundType.ON_SALES_REFUND.value()) &&
                            !Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
                        //updateShipmentItemRefundQuantity(skuOfRefund.getSkuCode(), shipmentItem.getQuantity(), shipmentItems);
                        updateShipmentItemRefundQuantity(refundItem, shipmentItem.getQuantity(), shipmentItems);
                    }
                    if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.CANCELED.getValue())) {
                        //TODO 更新发货单明细
                        shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);
                    }

                    refundItem.setSkuOrderId(shipmentItem.getSkuOrderId());

                    //获取skuCode
                    try {
                        SkuTemplate skuTemplate = this.findSkuTemplateBySkuCode(skuOrder.getSkuCode());
                        refundItem.setAttrs(skuTemplate.getAttrs());
                    } catch (Exception e) {
                        log.error("find sku template failed,skuCode is {},caused by {}", skuOrder.getSkuCode(),
                            Throwables.getStackTraceAsString(e));
                    }
                    refundItem.setSkuName(skuOrder.getItemName());
                    refundItemList.add(refundItem);
                    extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(refundItemList));
                    Map<String, String> tagMap = Maps.newHashMap();
                    tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.THIRD.value()));
                    //VIP OXO售后单不提供金额，从发货单取
                    if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())
                            && (Objects.isNull(refund.getFee()) || refund.getFee().equals(0L))) {

                        refund.setFee(shipmentItem.getCleanPrice() * refundItem.getApplyQuantity().longValue());
                    } else {
                        if (shipmentItem.getQuantity() < skuOrder.getQuantity()) {
                            refund.setFee(refund.getFee() * shipmentItem.getQuantity() / skuOrder.getQuantity());
                        }
                    }
                    refund.setExtra(extraMap);
                    refund.setTags(tagMap);
                    refundList.add(refund);
                }
            }

        } catch (Exception e) {
            log.error("create refund find error,shopOrderId is {},caused by {}", shopOrder.getId(),
                Throwables.getStackTraceAsString(e));
        }
        return refundList;
    }

    @Override
    protected void fillLogisticsInfo(Refund refund, String shipmentSerialNo, String shipmentCorpCode,
                                     String shipmentCorpName) {


        psAfterSaleReceiverHelper.fillLogisticsInfo(refund,shipmentSerialNo,shipmentCorpCode,shipmentCorpName);
    }

    @Override
    protected Integer toParanaRefundType(OpenClientAfterSaleType type) {
        switch (type) {
            case IN_SALE_REFUND:
                return MiddleRefundType.ON_SALES_REFUND.value();
            case AFTER_SALE_ONLY_REFUND:
                return MiddleRefundType.AFTER_SALES_REFUND.value();
            case AFTER_SALE:
                return MiddleRefundType.AFTER_SALES_RETURN.value();
            case EXCHANGE:
                return MiddleRefundType.AFTER_SALES_CHANGE.value();
            default:
                log.error("open client after sale type:{} invalid", type.name());
                throw new OpenClientException(500, "open.client.after.type.invalid");
        }
    }

    @Override
    protected Integer toParanaRefundStatus(OpenClientAfterSaleStatus status) {
        switch (status) {
            case SELLER_AGREE_BUYER:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case WAIT_BUYER_RETURN_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case WAIT_SELLER_CONFIRM_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case SUCCESS:
                return MiddleRefundStatus.REFUND.getValue();
            case RETURN_CLOSED:
                return MiddleRefundStatus.CANCELED.getValue();
            default:
                log.error("open client after sale status:{} invalid", status.name());
                throw new OpenClientException(500, "open.client.after.status.invalid");

        }
    }

    @Override
    protected void updateRefund(Refund refund, OpenClientAfterSale afterSale) {
        //如果这个时候拉取过来的售后单是用户自己取消且为退货类型的可以更新售后单的状态
        if (afterSale.getStatus() == OpenClientAfterSaleStatus.RETURN_CLOSED
            && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
            //判断售后单状态
            Flow flow = flowPicker.pickAfterSales();
            //这个时候的状态可能为待完善,待同步恒康,同步恒康失败
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation())
                || flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.SYNC_HK.toOrderOperation())) {
                //直接售后单的状态为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(),
                    refund.getStatus(), MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                        refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                } else {
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }
            //已经同步恒康
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                } else {
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }
        }
        if (afterSale.getStatus() != OpenClientAfterSaleStatus.SUCCESS) {
            return;
        }
        //仅退款的订单只有同步完成之后才会更新售后状态
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())
            && !Objects.equals(refund.getStatus(), MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue())) {
            return;
        }
        //退货退款单只有订单退货完成待退款才可以更新售后状态
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())
            && !Objects.equals(refund.getStatus(), MiddleRefundStatus.SYNC_ECP_SUCCESS_WAIT_REFUND.getValue())) {
            return;
        }
        Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(),
            refund.getStatus(), MiddleRefundStatus.REFUND.getValue());
        if (!updateR.isSuccess()) {
            log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                refund.getId(), MiddleRefundStatus.REFUND.getValue(), afterSale, updateR.getError());
        }
    }

    /**
     * 获取存在skuCode的发货单
     *
     * @param shopOrderId 店铺订单id
     * @param skuCode     商品条码
     * @param quantity    申请售后的数量
     * @return
     */
    private Shipment findShipmentByOrderInfo(long shopOrderId, String skuCode, Integer quantity) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,
            OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
            filter(
                shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects
                    .equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(
            Collectors.toList());
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<ShipmentItem> shipmentItemFilters = shipmentItems.stream().
                filter(Objects::nonNull).filter(shipmentItem -> Objects.equals(shipmentItem.getSkuCode(), skuCode))
                .filter(shipmentItem -> (shipmentItem.getQuantity() >= quantity)).collect(Collectors.toList());
            if (shipmentItemFilters.size() > 0) {
                return shipment;
            }
        }
        return null;
    }

    /**
     * 获取skuOrder对应的发货单
     *
     * @param shopOrderId
     * @param skuOrder
     * @return
     */
    private Shipment findShipmentByOrderInfo(long shopOrderId, SkuOrder skuOrder) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) &&
                        !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
                .collect(Collectors.toList());
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            long count = shipmentItems.stream()
                    .filter(shipmentItem -> SkuCodeUtil.compareCombineCode(shipmentItem, skuOrder))
                    .count();
            if (count > 0) {
                return shipment;
            }
        }
        return null;
    }

    /**
     * 获取存在skuCode的发货单
     *
     * @param shopOrderId 店铺订单id
     * @param skuCode     商品条码
     * @return
     */
    private List<Shipment> findShipmentByOrderInfo(long shopOrderId, String skuCode) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,
            OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> availShipments = Lists.newArrayList();
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
            filter(
                shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects
                    .equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
            .collect(Collectors.toList());
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<ShipmentItem> shipmentItemFilters = shipmentItems.stream().
                filter(Objects::nonNull).filter(shipmentItem -> Objects.equals(shipmentItem.getSkuCode(), skuCode))
                .collect(Collectors.toList());
            if (shipmentItemFilters.size() > 0) {
                availShipments.add(shipment);
            }
        }
        return availShipments;
    }

    /**
     * 获取存在skuCode的发货单
     *
     * @param shopOrderId
     * @param skuOrder
     * @return
     */
    private List<Shipment> findShipmentBySkuOrderInfo(long shopOrderId, SkuOrder skuOrder) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> availShipments = Lists.newArrayList();
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
            filter(
                shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects
                    .equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
            .collect(Collectors.toList());
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            long count = shipmentItems.stream()
                    .filter(shipmentItem -> SkuCodeUtil.compareCombineCode(shipmentItem, skuOrder))
                    .count();
            if (count > 0) {
                availShipments.add(shipment);
            }
        }
        return availShipments;
    }

    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode, Integer refundQuantity,
                                                  List<ShipmentItem> shipmentItems) {
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (Objects.equals(skuCode, shipmentItem.getSkuCode())) {
                shipmentItem.setRefundQuantity(
                    (shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) + refundQuantity);
            }
        }
    }

    /**
     * 更新发货单商品中的已退货数量
     *
     * @param refundItem
     * @param refundQuantity
     * @param shipmentItems
     */
    private void updateShipmentItemRefundQuantity(RefundItem refundItem, Integer refundQuantity, List<ShipmentItem> shipmentItems) {
        shipmentItems.forEach(shipmentItem -> {
            if (SkuCodeUtil.compareCombineCode(shipmentItem, refundItem)) {
                shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) + refundQuantity);
            }
        });
    }

    private SkuTemplate findSkuTemplateBySkuCode(String skuCode) {
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                skuCode, findR.getError());
            throw new ServiceException("find.skuTemplate.failed");
        }
        return findR.getResult().get();

    }

    /**
     * 商品是否存在未结束的换货单
     *
     * @param shopOrderId 店铺订单id
     * @param afterSale   售后单
     * @return
     */
    @Override
    protected boolean existExchanges(long shopOrderId, OpenClientAfterSale afterSale) {
        List<OpenClientAfterSaleItem> afterSaleItems = afterSale.getOpenClientAfterSaleItems();
        //判断是否存在换货单（根据订单)
        Response<List<Refund>> response = refundReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find refund failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("order.refund.find.fail");
        }
        //换货单 未取消 未删除 未关闭
        List<Refund> exchanges = response.getResult().stream().filter(Objects::nonNull).
                filter(t -> Objects.equals(t.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value()) && !Objects.equals(t.getStatus(), MiddleRefundStatus.CANCELED.getValue()) && !Objects.equals(t.getStatus(), MiddleRefundStatus.DELETED.getValue()) && !Objects.equals(t.getStatus(), MiddleRefundStatus.EXCHANGE_CLOSED.getValue())).collect(Collectors.toList());
        //新拉的售后单商品
        List<String> refundOutSkuCodes = Lists.newArrayList();
        afterSaleItems.stream().forEach(afterSaleItem -> {
            refundOutSkuCodes.add(afterSaleItem.getOpenSkuId());
        });
        for (Refund exchange : exchanges) {
            List<RefundItem> exchangeItems = MAPPER.fromJson(exchange.getExtra().get(TradeConstants.REFUND_ITEM_INFO),
                MAPPER.createCollectionType(List.class, RefundItem.class));
            //已有的换货单商品
            List<String> exchangeSkuCodes = Lists.newArrayList();
            exchangeItems.stream().forEach(exchangeItem -> {
                exchangeSkuCodes.add(exchangeItem.getOutSkuCode());
            });
            //取交集 如果有交集说明退货商品有未处理的换货单 不拉取该换货单
            exchangeSkuCodes.retainAll(refundOutSkuCodes);
            if (exchangeSkuCodes.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isExpectedStatus(OpenClientAfterSale openClientAfterSale) {

        if (Arguments.isNull(openClientAfterSale.getStatus()) || Arguments.isNull(openClientAfterSale.getType())) {
            return false;
        }
        log.info("check refund order status,outOrderId={},currentStatus={},type={},shopId={}",new Object[]{openClientAfterSale.getOpenOrderId(),openClientAfterSale.getStatus(),openClientAfterSale.getType(),openClientAfterSale.getOpenShopId()});

        String openClientAfterSaleType = openClientAfterSale.getType().name();

        if (!Objects.equals(openClientAfterSaleType, OpenClientAfterSaleType.EXCHANGE.name()) && !Objects.equals(
            openClientAfterSaleType, OpenClientAfterSaleType.AFTER_SALE.name())) {
            return super.isExpectedStatus(openClientAfterSale);
        }
        if (psAfterSaleReceiverHelper.filterInitStatusWhenPullAfterSaleOrder(openClientAfterSale)) {
            return psAfterSaleReceiverHelper.isExpectedStatus(openClientAfterSale);
        } else {
            return super.isExpectedStatus(openClientAfterSale);
        }

    }
}