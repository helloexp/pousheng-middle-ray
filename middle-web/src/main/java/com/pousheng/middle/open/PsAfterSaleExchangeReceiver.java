package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleChangeReceiveInfo;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.aftersale.component.DefaultAfterSaleExchangeReceiver;
import io.terminus.open.client.center.job.aftersale.dto.SkuOfRefundExchange;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.dto.OpenClientOrderConsignee;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by caj on 8/12/18.
 */
@Component
@Slf4j
public class PsAfterSaleExchangeReceiver extends DefaultAfterSaleExchangeReceiver {

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
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private SkuTemplateDao skuTemplateDao;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    public PsAfterSaleExchangeReceiver(PoushengMiddleSpuService middleSpuService) {
        this.middleSpuService = middleSpuService;
    }

    @Override
    protected void fillSkuInfo(ShopOrder shopOrder, Refund refund,SkuOfRefundExchange skuOfRefundExchange) {
        log.info("psAfterSaleReceiver skuCode is ({})", skuOfRefundExchange.toString());
        log.info("psAfterSaleReceiver shopOrderId is ({})", shopOrder.getId());
        log.info("fill address {}", refund.toString());

        RefundExtra refundExtra = new RefundExtra();
        //关联单号
        refundExtra.setReleOrderNo(shopOrder.getOrderCode());
        //关联单号类型
        refundExtra.setReleOrderType(1);
        //售后单添加一个锁标识
        refund.setTradeNo(TradeConstants.AFTER_SALE_EXHCANGE_UN_LOCK);

        refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());

        fillRefundItem(shopOrder, refund, skuOfRefundExchange, refundExtra);

        log.info("exchange message {}", refund.toString());
    }

    /**
     * @param shopOrder
     * @param refund
     * @param skuOfRefundExchange
     * @param refundExtra
     */
    private void fillRefundItem(ShopOrder shopOrder, Refund refund, SkuOfRefundExchange skuOfRefundExchange, RefundExtra refundExtra) {
        try {
            List<RefundItem> refundItemList = Lists.newArrayList();
            Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();

                SkuOrder skuOrder;
                if (StringUtils.hasText(skuOfRefundExchange.getChannelSkuId())) {
                    skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndOutSkuId(shopOrder.getId(), skuOfRefundExchange.getChannelSkuId());
                    skuOfRefundExchange.setSkuCode(skuOrder.getSkuCode());
                }else {
                    skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(shopOrder.getId(), skuOfRefundExchange.getSkuCode());
                }
                //查询需要售后的发货单
                Shipment shipment = this.findShipmentByOrderInfo(shopOrder.getId(), skuOfRefundExchange.getSkuCode(), skuOrder.getQuantity());

                if (!Objects.isNull(shipment)) {
                    refundExtra.setShipmentId(shipment.getShipmentCode());
                    //添加售后仓库
                    try {
                        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
                        String warehouseId = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID, openShop);
                        String warehouseName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME, openShop);
                        refundExtra.setWarehouseId(Long.valueOf(warehouseId));
                        refundExtra.setWarehouseName(warehouseName);
                    } catch (ServiceException e) {
                        log.error("find warehouse info failed,caused by {}", Throwables.getStackTraceAsString(e));
                    }
                }else{
                    log.error("find shipment info failed,shopOrderId is {},skuCode is {},quantity is", shopOrder.getId(),skuOfRefundExchange.getSkuCode(),skuOrder.getQuantity());
                }

                RefundItem refundItem = new RefundItem();
                if (!Objects.isNull(shipment)) {
                    List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                    ShipmentItem shipmentItem = shipmentItems
                            .stream().filter(shipmentItem1 ->
                                    Objects.equals(shipmentItem1.getSkuCode(), skuOfRefundExchange.getSkuCode())).collect(Collectors.toList()).get(0);
                    if ((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) > 0) {
                        log.warn("this refund item has been applied,refundSkuCode is {}", skuOfRefundExchange.getSkuCode());
                        refund.setStatus(MiddleRefundStatus.DELETED.getValue());
                        refund.setSellerNote("系统：订单商品已产生售后，当前订单不同步ERP");
                    }else{
                        refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
                        refundItem.setSkuPrice(shipmentItem.getSkuPrice());
                        refundItem.setSkuDiscount(shipmentItem.getSkuDiscount());
                        refundItem.setCleanFee(shipmentItem.getCleanPrice()*skuOfRefundExchange.getRefundNum());
                        refundItem.setCleanPrice(shipmentItem.getCleanPrice());
                        refundItem.setAlreadyHandleNumber(0);
                        List<SkuAttribute> attrs = shipmentItem.getAttrs();
                        refundItem.setAttrs(attrs);
                        refundItem.setItemId(shipmentItem.getItemId());
                        refundItem.setApplyQuantity(skuOfRefundExchange.getRefundNum());
                        refundItem.setSharePlatformDiscount(shipmentItem.getSharePlatformDiscount());

                        refund.setFee(shipmentItem.getCleanPrice()*skuOfRefundExchange.getRefundNum().longValue());

                        updateShipmentItemRefundQuantity(skuOfRefundExchange.getSkuCode(), skuOfRefundExchange.getRefundNum(), shipmentItems);

                        if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.CANCELED.getValue())) {
                            shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);
                        }
                    }
                }
                refundItem.setSkuCode(skuOrder.getSkuCode());
                refundItem.setSkuOrderId(skuOrder.getId());
                refundItem.setOutSkuCode(skuOrder.getOutSkuId());
                //获取skuCode
                try {
                    SkuTemplate skuTemplate = this.findSkuTemplateBySkuCode(skuOrder.getSkuCode());
                    if (skuTemplate!=null){
                        refundItem.setAttrs(skuTemplate.getAttrs());
                        refundItem.setSkuName(skuTemplate.getName());
                    }
                } catch (Exception e) {
                    log.error("find sku template failed,skuCode is {},caused by {}", skuOrder.getSkuCode(), Throwables.getStackTraceAsString(e));
                }
                refundItem.setSkuName(skuOrder.getItemName());
                refundItemList.add(refundItem);

            extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(refundItemList));
            Map<String, String> tagMap = Maps.newHashMap();
            tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.THIRD.value()));

            List<RefundItem> changeRefundItems = Lists.newArrayList();
            RefundItem changeRefundItem = new RefundItem();
            changeRefundItem.setApplyQuantity(skuOfRefundExchange.getExchangeNum());
            changeRefundItem.setAlreadyHandleNumber(0);
            changeRefundItem.setSkuCode(skuOfRefundExchange.getExchangeSkuCode());
            //查询换货商品信息

            Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuOfRefundExchange.getExchangeSkuCode()));
            if(!skuTemplateRes.isSuccess()){
                log.error("find sku template by sku code:{} fail,error:{}",skuOfRefundExchange.getExchangeSkuCode(),skuTemplateRes.getError());
            }
            //获取有效的货品条码
            java.util.Optional<SkuTemplate> skuTemplateOptional= skuTemplateRes.getResult().stream().filter(skuTemplate -> !Objects.equals(skuTemplate.getStatus(),-3)).findAny();
            if(!skuTemplateOptional.isPresent()){
                log.error("not find sku template by sku code:{}",skuOfRefundExchange.getExchangeSkuCode());
            }else{
                SkuTemplate sku = skuTemplateOptional.get();
                changeRefundItem.setSkuName(sku.getName());
                changeRefundItem.setAttrs(sku.getAttrs());
                changeRefundItem.setSkuPrice(refundItem.getSkuPrice());
                changeRefundItem.setCleanPrice(refundItem.getCleanPrice());
                changeRefundItem.setCleanFee(refundItem.getCleanFee());
                changeRefundItem.setSkuDiscount(refundItem.getSkuDiscount());
                changeRefundItem.setFee(refundItem.getFee());
            }
            changeRefundItems.add(changeRefundItem);
            extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, mapper.toJson(changeRefundItems));

            refund.setExtra(extraMap);
            refund.setTags(tagMap);
        } catch (Exception e) {
            log.error("create refund find error,shopOrderId is {},caused by {}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
        }
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
            case WAIT_BUYER_RETURN_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case EXCHANGE_CLOSED:
                return MiddleRefundStatus.EXCHANGE_CLOSED.getValue();
            case EXCHANGE_SUCCESS:
                return MiddleRefundStatus.EXCHANGE_SUCCESS.getValue();
            case WAIT_SELLER_CONFIRM_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case WAIT_SELLER_SEND_GOODS:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();
            case EXCHANGE_TO_REFUND:
                return MiddleRefundStatus.WAIT_HANDLE.getValue();

            default:
                log.error("open client after sale status:{} invalid", status.name());
                throw new OpenClientException(500, "open.client.after.status.invalid");

        }
    }

    @Override
    protected void updateExchange(Refund refund, OpenClientAfterSale afterSale) {
        //判断售后单状态
        Flow flow = flowPicker.pickAfterSales();
        //换货关闭
        if (Objects.equals(afterSale.getStatus(),OpenClientAfterSaleStatus.EXCHANGE_CLOSED)
                && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {//换货关闭
            //这个时候的状态可能为待完善,待同步恒康,同步恒康失败（取消中台售后单 回滚发货单）
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation())
                    || flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.SYNC_HK.toOrderOperation())) {
                //直接售后单的状态为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(), refund.getStatus(), MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                            refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                } else {
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }
            //已经同步恒康（取消恒康售后单 取消中台售后单 回滚发货单） 客服介入
            /*if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturnCancel(refund);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                } else {
                    Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(), refund.getStatus(), MiddleRefundStatus.CANCELED.getValue());
                    if (!updateR.isSuccess()) {
                        log.error("fail to update refund(id={}) status to {}cause:{}",
                                refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                    } else {
                        //回滚发货单的数量
                        refundWriteLogic.rollbackRefundQuantities(refund);
                    }
                }
                return;
            }*/
        }
        //换货成功
        if (Objects.equals(afterSale.getStatus() , OpenClientAfterSaleStatus.EXCHANGE_SUCCESS)) {
            //中台售后单为已发货 更新中台状态 为已完成？

            return;
        }
        //换转退
        if (Objects.equals(afterSale.getStatus() , OpenClientAfterSaleStatus.EXCHANGE_TO_REFUND)) {
            //中台售后单状态为待完善时(关闭售后单)
            if(flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation())){
                //直接售后单的状态改为为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(), refund.getStatus(), MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                            refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                } else {
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }

            //中台售后单状态为待同步恒康,同步恒康失败时(取消售后单，取消发货单占库),拉取新的退货退款单。
            if(flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.SYNC_HK.toOrderOperation())){//已完善没有同步恒康
                //直接售后单的状态改为为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(), refund.getStatus(), MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                            refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                } else {
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                    //占库单取消
                    refundWriteLogic.cancelAfterSaleOccupyShipments(refund.getId());
                }
                return;
            }
            //售后已经同步恒康后由客服手动操作换转退

            return;
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
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
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

    private SkuTemplate findSkuTemplateBySkuCode(String skuCode) {
 //       Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
//        if (!findR.isSuccess()) {
//            log.error("fail to find sku template by skuCode={},cause:{}",
//                    skuCode, findR.getError());
//        }
//        return findR.getResult().get();
        try {
            List<SkuTemplate> skuTemplates = skuTemplateDao.findBySkuCode(skuCode);
            if (CollectionUtils.isEmpty(skuTemplates)) {
                log.warn("sku template not found where skuCode={}", skuCode);
            }
            return skuTemplates.get(0);
        } catch (Exception e) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuCode, Throwables.getStackTraceAsString(e));
        }
        return null;
    }
    @Override
    protected void fillExchangeReceiverInfo(ShopOrder shopOrder,Refund refund, OpenClientAfterSale openClientAfterSale) {
        log.info("fill address {}", refund.toString());
        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();
        //完善换货发货地址信息
        if (Objects.nonNull(openClientAfterSale.getConsignee())) {
            OpenClientOrderConsignee consignee = openClientAfterSale.getConsignee();
            MiddleChangeReceiveInfo receiveInfofo = new MiddleChangeReceiveInfo();
            receiveInfofo.setReceiveUserName(consignee.getName());
            receiveInfofo.setMobile(consignee.getMobile());
            receiveInfofo.setPhone(consignee.getTelephone());
            receiveInfofo.setProvince(consignee.getProvince());
            receiveInfofo.setCity(consignee.getCity());
            receiveInfofo.setRegion(consignee.getRegion());
            receiveInfofo.setDetail(consignee.getDetail());
            extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO, mapper.toJson(receiveInfofo));
        }

        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order receive info by order id:{} fial,error:{}",shopOrder.getId(),response.getError());
            //throw new ServiceException("find.order.receive.info.failed");
        }
        List<ReceiverInfo> receiverInfos = response.getResult();
        if(CollectionUtils.isEmpty(receiverInfos)){
            log.error("not find receive info by order id:{}",shopOrder.getId());
            //throw new ServiceException("order.receive.info.not.exist");
        }

        RefundExtra refundExtrainfo = mapper.fromJson(extraMap.get(TradeConstants.REFUND_EXTRA_INFO), RefundExtra.class);
        refundExtrainfo.setReceiverInfo(receiverInfos.get(0));
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtrainfo));
        refund.setExtra(extraMap);
    }

    /**
     * 判断订单中skuCode的商品是否经过数量级拆单
     *
     * @param shopOrderId 店铺订单id
     * @param skuCode     商品条码
     * @return
     */
    @Override
    protected boolean existMultipleShipments(long shopOrderId, String skuCode) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        int num = 0;
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<ShipmentItem> shipmentItemFilters = shipmentItems.stream().
                    filter(Objects::nonNull).filter(shipmentItem -> Objects.equals(shipmentItem.getOutSkuCode(), skuCode)).collect(Collectors.toList());
            if (shipmentItemFilters.size() > 0) {
                num++;
            }
        }
        if(num>1){//数量级拆单
           return true;
        }
        return false;
    }
    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode, Integer refundQuantity, List<ShipmentItem> shipmentItems) {
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (Objects.equals(skuCode, shipmentItem.getSkuCode())) {
                shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) + refundQuantity);
            }
        }
    }

}
