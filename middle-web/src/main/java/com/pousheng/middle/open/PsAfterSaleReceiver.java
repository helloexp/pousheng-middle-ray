package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.aftersale.component.DefaultAfterSaleReceiver;
import io.terminus.open.client.center.job.aftersale.dto.SkuOfRefund;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private WarehouseReadService warehouseReadService;
    @Autowired
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;
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




    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    public PsAfterSaleReceiver(PoushengMiddleSpuService middleSpuService) {
        this.middleSpuService = middleSpuService;
    }

    @Override
    protected void fillSkuInfo(ShopOrder shopOrder, Refund refund, SkuOfRefund skuOfRefund) {
        log.info("psAfterSaleReceiver skuCode is ({})",skuOfRefund.getSkuCode());
        log.info("psAfterSaleReceiver shopOrderId is ({})",shopOrder.getId());

        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(shopOrder.getId());
        //塞入地址信息
        RefundExtra refundExtra = new RefundExtra();
        refundExtra.setReceiverInfo(receiverInfo);

        if(Objects.equals(MiddleRefundType.ON_SALES_REFUND.value(),refund.getRefundType())){
            //借用tradeNo字段来标记售中退款的逆向单是否已处理
            refund.setTradeNo(TradeConstants.REFUND_WAIT_CANCEL);
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }

        //判断售后单对应的是否是天猫订单
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())&&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())){
            //如果天猫拉取过来的仅退款的售后单就是success,这个时候中台做一下特殊处理
            if (Objects.equals(refund.getStatus(),MiddleRefundStatus.REFUND.getValue())){
                //判断此时的订单以及发货单的状态
                refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
                //判断是否是售中退款
                if (orderReadLogic.isOnSaleRefund(shopOrder.getId())){
                    refund.setRefundType(MiddleRefundType.ON_SALES_REFUND.value());
                    refund.setTradeNo(TradeConstants.REFUND_WAIT_CANCEL);
                }
            }
        }
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())&&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
            //如果天猫拉取过来的退货退款的售后单就是success,这个时候中台做一下特殊处理
                refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }
        //苏宁售后近退款单子到中台订单状态做初始化
        if (Objects.equals(shopOrder.getOutFrom(),MiddleChannel.SUNING.getValue())&&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())){
                refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }
        if (!StringUtils.hasText(skuOfRefund.getSkuCode())) {
            return;
        }
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuOfRefund.getSkuCode());
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuOfRefund.getSkuCode(), findR.getError());
            return;
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();
        if (!skuTemplateOptional.isPresent()) {
            return;
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();

        SkuOrder skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(shopOrder.getId(), skuOfRefund.getSkuCode());
        //查询需要售后的发货单
        Shipment shipment = this.findShipmentByOrderInfo(shopOrder.getId(), skuOfRefund.getSkuCode(), skuOrder.getQuantity());

        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();

        if (!Objects.isNull(shipment)) {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            refundExtra.setShipmentId(shipment.getId());
            //添加售后仓库
            try{
                OpenShop openShop=orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
                String warehouseId=orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID,openShop);
                String warehouseName=orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME,openShop);
                refundExtra.setWarehouseId(Long.valueOf(warehouseId));
                refundExtra.setWarehouseName(warehouseName);
                //表明售后单的信息已经全部完善
                extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG,"0");
            }catch (ServiceException e){
                log.error("find warehouse info failed,caused by {}",e.getMessage());
            }
        }
        RefundItem refundItem = new RefundItem();
        if (!Objects.isNull(shipment)) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            ShipmentItem shipmentItem = shipmentItems
                    .stream().filter(shipmentItem1 ->
                            Objects.equals(shipmentItem1.getSkuCode(), skuOfRefund.getSkuCode())).collect(Collectors.toList()).get(0);
            if ((shipmentItem.getRefundQuantity()==null?0:shipmentItem.getRefundQuantity())>0){
                log.warn("this refund item has been applied,refundSkuCode is {}",skuOfRefund.getSkuCode());
                refund.setStatus(MiddleRefundStatus.DELETED.getValue());
                return;
            }
            refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
            refundItem.setSkuPrice(shipmentItem.getSkuPrice());
            refundItem.setSkuDiscount(shipmentItem.getSkuDiscount());
            refundItem.setCleanFee(shipmentItem.getCleanFee());
            refundItem.setCleanPrice(shipmentItem.getCleanPrice());
            refundItem.setAlreadyHandleNumber(shipmentItem.getQuantity());
            refundItem.setAttrs(shipmentItem.getAttrs());
            refundItem.setItemId(shipmentItem.getItemId());
            refundItem.setApplyQuantity(shipmentItem.getQuantity());
            updateShipmentItemRefundQuantity(skuOfRefund.getSkuCode(), shipmentItem.getQuantity(), shipmentItems);
            //更新发货单商品中的已退货数量
            Map<String, String> shipmentExtraMap = shipment.getExtra();
            shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(shipmentItems));
            //如果拉取下来的售后单是已取消，则不需要更新已退货数量
            if (!Objects.equals(refund.getStatus(),MiddleRefundStatus.CANCELED.getValue())){
                shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
            }
        }

        refundItem.setSkuCode(skuOrder.getSkuCode());
        refundItem.setSkuOrderId(skuOrder.getId());
        refundItem.setOutSkuCode(skuOrder.getOutSkuId());
        refundItem.setAttrs(skuTemplate.getAttrs());
        refundItem.setSkuName(skuOrder.getItemName());

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(Lists.newArrayList(refundItem)));
        Map<String,String> tagMap = refund.getTags() != null ? refund.getTags() : Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.THIRD.value()));

        refund.setExtra(extraMap);
        refund.setTags(tagMap);
    }


    @Override
    protected void fillLogisticsInfo(Refund refund, String shipmentSerialNo, String shipmentCorpCode, String shipmentCorpName) {

        if(Strings.isNullOrEmpty(shipmentSerialNo)){
            return;
        }

        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();
        RefundExtra refundExtra = null;
        try{
            refundExtra = refundReadLogic.findRefundExtra(refund);
        }catch (JsonResponseException e){
            log.error("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_EXTRA_INFO);
            return;
        }
        refundExtra.setShipmentSerialNo(shipmentSerialNo);
        //转换为中台的物流信息

        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setPoushengCode(shipmentCorpCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            return;
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by poushengCode:{}", shipmentCorpCode);
            return;
        }
        ExpressCode expressCode = response.getResult().getData().get(0);
        refundExtra.setShipmentCorpName(expressCode.getName());
        refundExtra.setShipmentCorpCode(expressCode.getOfficalCode());

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));

        refund.setExtra(extraMap);

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
        if (afterSale.getStatus()==OpenClientAfterSaleStatus.RETURN_CLOSED
                && Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
            //判断售后单状态
            Flow flow = flowPicker.pickAfterSales();
            //这个时候的状态可能为待完善,待同步恒康,同步恒康失败
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation())
                    || flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.SYNC_HK.toOrderOperation())){
                //直接售后单的状态为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatus(refund.getId(),MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                            refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                }else{
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }
            //已经同步恒康
            if (flow.operationAllowed(refund.getStatus(),MiddleOrderEvent.CANCEL_HK.toOrderOperation())){
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                }else{
                    //回滚发货单的数量
                    refundWriteLogic.rollbackRefundQuantities(refund);
                }
                return;
            }
        }
        if (afterSale.getStatus() != OpenClientAfterSaleStatus.SUCCESS) {
            return;
        }
        //淘宝苏宁仅退款的订单做特殊处理
        if (Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())
                &&!Objects.equals(refund.getStatus(),MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue())){
            return;
        }
        //淘宝的退货退款单只有订单退货完成待退款才可以更新发货单状态
        if (Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())
                &&!Objects.equals(refund.getStatus(),MiddleRefundStatus.SYNC_ECP_SUCCESS_WAIT_REFUND.getValue())){
            return;
        }
        Response<Boolean> updateR = refundWriteService.updateStatus(refund.getId(), MiddleRefundStatus.REFUND.getValue());
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
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new ServiceException("find.shipment.failed");
        }
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
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

    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode, Integer refundQuantity, List<ShipmentItem> shipmentItems) {
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (Objects.equals(skuCode, shipmentItem.getSkuCode())) {
                shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity()==null?0:shipmentItem.getRefundQuantity()) + refundQuantity);
            }
        }
    }


}
