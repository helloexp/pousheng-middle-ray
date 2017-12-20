package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.open.erp.ErpOpenApiClient;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.events.trade.StepOrderNotifyHkEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.PoushengGiftActivityReadLogic;
import com.pousheng.middle.web.order.component.PsGiftActivityStrategy;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderConsignee;
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.InvoiceWriteService;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by cp on 7/25/17
 */
@Component
@Slf4j
public class PsOrderReceiver extends DefaultOrderReceiver {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private InvoiceWriteService invoiceWriteService;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private PoushengGiftActivityReadLogic poushengGiftActivityReadLogic;

    @Autowired
    private PsGiftActivityStrategy psGiftActivityStrategy;

    @Autowired
    private ErpOpenApiClient erpOpenApiClient;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private WarehouseAddressReadService warehouseAddressReadService;

    @Autowired
    private ReceiverInfoCompleter receiverInfoCompleter;

    /**
     * 天猫加密字段占位符
     */
    private static final String ENCRYPTED_FIELD_PLACE_HOLDER = "#****";

    @Override
    protected Item findItemById(Long paranaItemId) {
        //TODO use cache

        Response<Spu> findR = spuReadService.findById(paranaItemId);
        if (!findR.isSuccess()) {
            log.error("fail to find spu by id={},cause:{}", paranaItemId, findR.getError());
            return null;
        }
        Spu spu = findR.getResult();

        Item item = new Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setMainImage(spu.getMainImage_());
        return item;
    }

    @Override
    protected Sku findSkuByCode(String skuCode) {
        //TODO use cache
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();
        if (!skuTemplateOptional.isPresent()) {
            return null;
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setPrice(skuTemplate.getPrice());
        sku.setSkuCode(skuTemplate.getSkuCode());
        sku.setItemId(skuTemplate.getSpuId());
        try {
            sku.setExtraPrice(skuTemplate.getExtraPrice());
        } catch (Exception e) {
            //ignore
        }
        sku.setImage(skuTemplate.getImage_());
        sku.setAttrs(skuTemplate.getAttrs());
        return sku;
    }

    @Override
    protected Integer toParanaOrderStatusForShopOrder(OpenClientOrderStatus clientOrderStatus) {
        return OpenClientOrderStatus.PAID.getValue();
    }

    @Override
    protected Integer toParanaOrderStatusForSkuOrder(OpenClientOrderStatus clientOrderStatus) {
        return OpenClientOrderStatus.PAID.getValue();
    }

    @Override
    protected void updateParanaOrder(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        if (openClientFullOrder.getStatus() == OpenClientOrderStatus.CONFIRMED) {
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(), MiddleOrderStatus.SHIPPED.getValue());
            if (skuOrders.size() == 0) {
                return;
            }
            for (SkuOrder skuOrder : skuOrders) {
                Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), MiddleOrderStatus.SHIPPED.getValue(), MiddleOrderStatus.CONFIRMED.getValue());
                if (!updateRlt.getResult()) {
                    log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                }
            }
            //判断订单的状态是否是已完成
            ShopOrder shopOrder1 = orderReadLogic.findShopOrderById(shopOrder.getId());
            if (!Objects.equals(shopOrder1.getStatus(), MiddleOrderStatus.CONFIRMED.getValue())) {
                log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order",
                        shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
            } else {
                //更新同步电商状态为已确认收货
                OrderOperation successOperation = MiddleOrderEvent.CONFIRM.toOrderOperation();
                Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                if (response.isSuccess()) {
                    //通知恒康发货单收货时间
                    NotifyHkOrderDoneEvent event = new NotifyHkOrderDoneEvent();
                    event.setShopOrderId(shopOrder.getId());
                    eventBus.post(event);
                }
            }
        }
        //如果是预售订单且订单没有发货，预售订单已经支付尾款，则通知订单将发货单发往恒康
        if (Objects.nonNull(openClientFullOrder.getIsStepOrder())&&openClientFullOrder.getIsStepOrder()){
          if (Objects.nonNull(openClientFullOrder.getStepStatus())&&openClientFullOrder.getStepStatus().getValue()== OpenClientStepOrderStatus.PAID.getValue()){
              Map<String, String> extraMap = shopOrder.getExtra();
              String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
              String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
              //判断订单是否是预售订单，并且判断预售订单是否已经付完尾款
              if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                  if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(stepOrderStatus,
                          String.valueOf(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue()))) {
                      //抛出一个事件更新预售订单状态
                      StepOrderNotifyHkEvent event = new StepOrderNotifyHkEvent();
                      event.setShopOrderId(shopOrder.getId());
                  }
              }
          }
        }
    }


    @Override
    protected RichOrder makeParanaOrder(OpenClientShop openClientShop,
                                        OpenClientFullOrder openClientFullOrder) {
        RichOrder richOrder = super.makeParanaOrder(openClientShop, openClientFullOrder);
        RichSkusByShop richSkusByShop = richOrder.getRichSkusByShops().get(0);

        if (OpenClientChannel.from(openClientShop.getChannel()) == OpenClientChannel.TAOBAO) {
            //这里先把buyer和mobile改为占位符，因为数据加密后长度很长，会导致数据库长度不够
            richOrder.getBuyer().setName(ENCRYPTED_FIELD_PLACE_HOLDER);
            if (richSkusByShop.getReceiverInfo() != null) {
                if (StringUtils.hasText(richSkusByShop.getReceiverInfo().getMobile())) {
                    richSkusByShop.getReceiverInfo().setMobile(ENCRYPTED_FIELD_PLACE_HOLDER);
                }
            }
        }

        //初始化店铺订单的extra
        Map<String, String> shopOrderExtra = richSkusByShop.getExtra() == null ? Maps.newHashMap() : richSkusByShop.getExtra();
        shopOrderExtra.put(TradeConstants.ECP_ORDER_STATUS, String.valueOf(EcpOrderStatus.WAIT_SHIP.getValue()));
        //添加绩效店铺编码,通过openClient获取
        shopOrderExtra.put(TradeConstants.ERP_PERFORMANCE_SHOP_CODE,openClientFullOrder.getPerformanceShopCode());
        //初始化订单待处理状态
        shopOrderExtra.put(TradeConstants.NOT_AUTO_CREATE_SHIPMENT_NOTE, String.valueOf(OrderWaitHandleType.WAIT_HANDLE.value()));
        //判断是否是预售订单
        if (Objects.nonNull(openClientFullOrder.getIsStepOrder())&&openClientFullOrder.getIsStepOrder()){
            shopOrderExtra.put(TradeConstants.IS_STEP_ORDER,String.valueOf(openClientFullOrder.getIsStepOrder().booleanValue()));
            if (Objects.nonNull(openClientFullOrder.getStepStatus())){
                shopOrderExtra.put(TradeConstants.STEP_ORDER_STATUS,String.valueOf(openClientFullOrder.getStepStatus().getValue()));
            }
        }
        richSkusByShop.setExtra(shopOrderExtra);

        //判断是否存在可用的赠品
        List<PoushengGiftActivity> activities = poushengGiftActivityReadLogic.findByStatus(PoushengGiftActivityStatus.WAIT_DONE.getValue());
        //获取赠品商品
        PoushengGiftActivity activity = psGiftActivityStrategy.getAvailGiftActivity(richSkusByShop,activities);  //最终选择的活动
        List<GiftItem> giftItems = null;
        if (Objects.nonNull(activity)){
            giftItems = poushengGiftActivityReadLogic.getGiftItem(activity);
        }else{
            giftItems = Lists.newArrayList();
        }
        List<RichSku> richSkus = richSkusByShop.getRichSkus();
        if (!Objects.isNull(giftItems) && giftItems.size()>0){
            for (GiftItem giftItem : giftItems){
                richOrder.setCompanyId(1L);//没有多余的字段了，companyId=1标记为这个订单中含有赠品
                RichSku richSku = new RichSku();
                Sku sku = this.findSkuByCode(giftItem.getSkuCode());
                if (sku==null){
                    sku.setId(giftItem.getSkuId());
                    sku.setSkuCode(giftItem.getSkuCode());
                    sku.setOuterSkuId(giftItem.getOutSKuId());
                    sku.setAttrs(giftItem.getAttrs());
                }
                Item item = this.findItemById(giftItem.getSpuId());
                if (item==null){
                    item.setId(giftItem.getItemId());
                    item.setName(giftItem.getItemName());
                }
                ParanaUser buyer = new ParanaUser();
                buyer.setId(richOrder.getBuyer().getId());
                buyer.setName(richOrder.getBuyer().getName());
                richSku.setOrderStatus(OpenClientOrderStatus.PAID.getValue());
                richSku.setQuantity(giftItem.getQuantity());
                richSku.setFee(0L);
                richSku.setOriginFee(0L);
                richSku.setDiscount(0L);
                richSku.setIntegral(0L);
                richSku.setBalance(0L);
                richSku.setShipFee(0L);
                richSku.setShipFeeDiscount(0L);
                richSku.setChannel(richSkusByShop.getChannel());
                richSku.setSku(sku);
                richSku.setItem(item);
                richSku.setShipmentType(1);//没有其他字段可用现在只能用shipmentType打标用来打标赠品
                richSku.setExtra(Maps.newHashMap());
                richSkus.add(richSku);
            }
        }else{
            richOrder.setCompanyId(2L);////没有多余的字段了，companyId=2标记为这个订单中不含有赠品
        }
        //初始化店铺子单extra
        richSkus.forEach(richSku -> {
            Map<String, String> skuExtra = richSku.getExtra();
            if (Objects.equals(richSku.getShipmentType(),1)&&Objects.nonNull(activity)){
                skuExtra.put(TradeConstants.GIFT_ACTIVITY_ID,String.valueOf(activity.getId()));
                skuExtra.put(TradeConstants.GIFT_ACTIVITY_NAME,activity.getName());
            }
            skuExtra.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(richSku.getQuantity()));
            richSku.setExtra(skuExtra);
        });
        richSkusByShop.setRichSkus(richSkus);
        //生成发票信息
        Long invoiceId = this.addInvoice(openClientFullOrder.getInvoice());
        richSkusByShop.setInvoiceId(invoiceId);
        return richOrder;
    }

    private Long addInvoice(OpenClientOrderInvoice openClientOrderInvoice) {
        try {
            //获取发票类型
            Integer invoiceType = Integer.valueOf(openClientOrderInvoice.getType());
            //获取抬头
            String title = openClientOrderInvoice.getTitle();
            //获取detail
            Map<String, String> detail = openClientOrderInvoice.getDetail();
            if (detail != null) {
                if (Objects.equals(invoiceType, 2)) {
                    //公司
                    detail.put("titleType", "2");
                }
            } else {
                detail = Maps.newHashMap();
                detail.put("type", String.valueOf(invoiceType));
            }

            Invoice newInvoice = new Invoice();
            newInvoice.setTitle(title);
            newInvoice.setStatus(1);
            newInvoice.setIsDefault(false);
            newInvoice.setDetail(detail);
            Response<Long> response = invoiceWriteService.createInvoice(newInvoice);
            if (!response.isSuccess()) {
                log.error("create invoice failed,caused by {}", response.getError());
                throw new ServiceException("create.invoice.failed");
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("create invoice failed,caused by {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected void saveParanaOrder(RichOrder richOrder) {
        super.saveParanaOrder(richOrder);

        for (RichSkusByShop richSkusByShop : richOrder.getRichSkusByShops()) {
            //如果是天猫订单，则发请求到端点erp，把收货地址信息同步过来
            if (OpenClientChannel.from(richSkusByShop.getOutFrom()) == OpenClientChannel.TAOBAO) {
                syncReceiverInfo(richSkusByShop);
            }
        }
    }


    @Override
    protected ReceiverInfo toReceiverInfo(OpenClientOrderConsignee consignee) {
        ReceiverInfo receiverInfo = super.toReceiverInfo(consignee);
        receiverInfoCompleter.complete(receiverInfo);
        return receiverInfo;
    }

    private void syncReceiverInfo(RichSkusByShop richSkusByShop) {
        try {
            erpOpenApiClient.doPost("order.receiver.sync",
                    ImmutableMap.of("shopId", richSkusByShop.getShop().getId(), "orderId", richSkusByShop.getOuterOrderId()));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                    richSkusByShop.getOuterOrderId(), richSkusByShop.getShop().getId(), Throwables.getStackTraceAsString(e));
        }
    }

}
