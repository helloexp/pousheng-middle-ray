package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.component.NotifyHkOrderDoneLogic;
import com.pousheng.middle.open.erp.ErpOpenApiClient;
import com.pousheng.middle.open.erp.TerminusErpOpenApiClient;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import com.pousheng.middle.web.biz.dto.ReceiverInfoDecryptDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.job.JdRedisHandler;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderConsignee;
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.InvoiceWriteService;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by cp on 7/25/17
 */
@Primary
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

    @Autowired
    private ShopReadService shopReadService;

    @RpcConsumer
    private PsShopReadService psShopReadService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private NotifyHkOrderDoneLogic notifyHkOrderDoneLogic;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private CompensateBizLogic compensateBizLogic;


    @Value("${redirect.fenxiao.erp.gateway:https://yymiddle.pousheng.com/api/qm/pousheng/wms-fenxiao}")
    private String poushengPagodaFenxiaoRedirectUrl;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private TerminusErpOpenApiClient terminusErpOpenApiClient;

    @Autowired
    private JdRedisHandler redisHandler;

    @Value("${redirect.erp.gateway}")
    private String poushengPagodaCommonRedirectUrl;

    /**
     * 天猫加密字段占位符
     */
    private static final String ENCRYPTED_FIELD_PLACE_HOLDER = "#****";

    @Override
    protected Item findItemById(Long paranaItemId) {
        //TODO use cache
        if (Objects.isNull(paranaItemId)){
            Item item = new Item();
            item.setId(0L);
            item.setName("默认商品(缺映射关系)");
            return item;
        }
        Response<Spu> findR = spuReadService.findById(paranaItemId);
        if (!findR.isSuccess()) {
            log.error("fail to find spu by id={},cause:{}", paranaItemId, findR.getError());
            return null;
        }
        Spu spu = findR.getResult();
        if (spu==null){
            Item item = new Item();
            item.setId(0L);
        }
        Item item = new Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setMainImage(spu.getMainImage_());
        return item;
    }

    @Override
    protected Sku findSkuByCode(String skuCode) {
        if (!StringUtils.hasText(skuCode)){
            Sku sku = new Sku();
            sku.setId(0L);
            sku.setName("默认商品(缺映射关系)");
            return sku;
        }
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();
        if (!skuTemplateOptional.isPresent()) {
            Sku sku = new Sku();
            sku.setId(0L);
            return sku;
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setItemId(skuTemplate.getSpuId());
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

        if (Arguments.isNull(clientOrderStatus)){
            throw new ServiceException("client.order.status.invalid");
        }
        switch (clientOrderStatus){
            case PAID:
                return OpenClientOrderStatus.PAID.getValue();
            case SHIPPED:
                return OpenClientOrderStatus.SHIPPED.getValue();
            case NOT_PAID:
                return OpenClientOrderStatus.NOT_PAID.getValue();
            case CANCEL:
                return OpenClientOrderStatus.CANCEL.getValue();
            case CONFIRMED:
                return OpenClientOrderStatus.CONFIRMED.getValue();
            default:
                return OpenClientOrderStatus.CANCEL.getValue();
        }
    }

    @Override
    protected Integer toParanaOrderStatusForSkuOrder(OpenClientOrderStatus clientOrderStatus) {
        //return OpenClientOrderStatus.PAID.getValue();
        return toParanaOrderStatusForShopOrder(clientOrderStatus);
    }

    @Override
    protected void updateParanaOrder(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        log.info("openClientFullOrder info {}", mapper.toJson(openClientFullOrder));
        if (openClientFullOrder.getStatus() == OpenClientOrderStatus.CONFIRMED) {

            //如果是mpos订单并且是自提,将待发货状态改为待收货
            if(shopOrder.getExtra().containsKey(TradeConstants.IS_ASSIGN_SHOP) && Objects.equals(shopOrder.getExtra().get(TradeConstants.IS_SINCE),"2")){
                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(), MiddleOrderStatus.WAIT_SHIP.getValue());
                if (skuOrders.size() == 0) {
                    return;
                }
                for (SkuOrder skuOrder : skuOrders) {
                    Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), MiddleOrderStatus.WAIT_SHIP.getValue(), MiddleOrderStatus.SHIPPED.getValue());
                    if (!updateRlt.getResult()) {
                        log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                    }
                }
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
                Shipment shipment = shipments.get(0);
                shipmentWiteLogic.updateStatusLocking(shipment,MiddleOrderEvent.SHIP.toOrderOperation());
            }

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
            noticeConfirm(shopOrder.getId());

        }

        //预售订单后续处理流程
        this.updateStepOrderInfo(shopOrder, openClientFullOrder);


    }

    public void noticeConfirm(Long shopOrderId) {
        //判断订单的状态是否是已完成
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (!Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue())) {
            log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order",
                    shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
        } else {
            //更新同步电商状态为已确认收货
            OrderOperation successOperation = MiddleOrderEvent.CONFIRM.toOrderOperation();
            Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            if (response.isSuccess()) {
                //通知恒康发货单收货时间
                Response<Long> result = notifyHkOrderDoneLogic.ctreateNotifyHkOrderDoneTask(shopOrder.getId());
                if (!result.isSuccess()) {
                    log.error("notifyHkOrderDoneLogic ctreateNotifyHkOrderDoneTask error shopOrderId ({})", shopOrder.getId());
                }
                //通知mpos店发发货单确认收货
                Response<Boolean> mposTaskResult = notifyHkOrderDoneLogic.createNotifyMposDoneTask(shopOrder.getId());
                if (!mposTaskResult.isSuccess()) {
                    log.error("notifyHkOrderDoneLogic ctreateNotifyMposDoneTask error shopOrderId ({})", shopOrder.getId());
                }
            }
        }
    }

    /**
     * 预售订单更新操作
     * @param shopOrder 中台订单
     * @param openClientFullOrder 订单更新内容
     */
    private void updateStepOrderInfo(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        //天猫平台处理，天猫订单的stepOrderStatus=1
        if (Objects.nonNull(openClientFullOrder.getIsStepOrder())&&openClientFullOrder.getIsStepOrder()){
            if (Objects.nonNull(openClientFullOrder.getStepStatus())
                    && openClientFullOrder.getStepStatus().getValue()== OpenClientStepOrderStatus.PAID.getValue()){
                Map<String, String> extraMap = shopOrder.getExtra();
                String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
                String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
                //判断订单是否是预售订单，并且判断预售订单是否已经付完尾款
                if (!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                    if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(stepOrderStatus,
                            String.valueOf(OpenClientStepOrderStatus.NOT_ALL_PAID.getValue()))) {
                        //抛出一个事件更新预售订单状态
                        log.info("start processing taobao step order,orderId {},isStepOrder {},currentStepOrderStatus {}"
                                ,shopOrder.getId(),isStepOrder,stepOrderStatus);
                        this.createStepOrderrNotifyErpTask(shopOrder.getId(),openClientFullOrder);
                    }
                }
            }
        }

        //京东平台处理,京东订单的stepOrderStatus=0
        if(Objects.equals(MiddleChannel.JD.getValue(), shopOrder.getOutFrom()) && Objects.isNull(openClientFullOrder.getIsStepOrder())) {
            Map<String, String> extraMap = shopOrder.getExtra();
            String isStepOrder = extraMap.get(TradeConstants.IS_STEP_ORDER);
            String stepOrderStatus = extraMap.get(TradeConstants.STEP_ORDER_STATUS);
            if(!StringUtils.isEmpty(isStepOrder) && Objects.equals(isStepOrder, "true")) {
                //抛出一个事件更新预售订单状态
                log.info("start processing jd step order,orderId {},isStepOrder {},currentStepOrderStatus {}"
                        ,shopOrder.getId(),isStepOrder,stepOrderStatus);
                if (!StringUtils.isEmpty(stepOrderStatus) && Objects.equals(stepOrderStatus,
                        String.valueOf(OpenClientStepOrderStatus.NOT_PAID.getValue()))) {
                    this.createStepOrderrNotifyErpTask(shopOrder.getId(),openClientFullOrder);
                }
            }
        }
    }


    @Override
    protected RichOrder makeParanaOrder(OpenClientShop openClientShop,
                                        OpenClientFullOrder openClientFullOrder) {
        RichOrder richOrder = super.makeParanaOrder(openClientShop, openClientFullOrder);

        RichSkusByShop richSkusByShop = richOrder.getRichSkusByShops().get(0);

        if ((OpenClientChannel.from(openClientShop.getChannel()) == OpenClientChannel.TAOBAO
                ||OpenClientChannel.from(openClientShop.getChannel()) == OpenClientChannel.TFENXIAO)
                &&richSkusByShop.getExtra() != null && !richSkusByShop.getExtra().containsKey("importOrder")) {
            //这里先把buyer和mobile改为占位符，因为数据加密后长度很长，会导致数据库长度不够
            richOrder.getBuyer().setName(ENCRYPTED_FIELD_PLACE_HOLDER);
            if (richSkusByShop.getReceiverInfo() != null) {
                if (StringUtils.hasText(richSkusByShop.getReceiverInfo().getMobile())) {
                    richSkusByShop.getReceiverInfo().setMobile(ENCRYPTED_FIELD_PLACE_HOLDER);
                }
            }
        }

        //初始化店铺订单的extra
        if(richSkusByShop.getExtra() != null && richSkusByShop.getExtra().containsKey(TradeConstants.IS_ASSIGN_SHOP)){

            Map<String,String> tempExtra = richSkusByShop.getExtra();
            tempExtra.put(TradeConstants.IS_ASSIGN_SHOP,richSkusByShop.getExtra().get(TradeConstants.IS_ASSIGN_SHOP));
            if(Objects.equals(tempExtra.get(TradeConstants.IS_ASSIGN_SHOP),"1")){
                //修改，mpos传来outerId
                String outerId = richSkusByShop.getExtra().get("assignShopOuterId");
                if(Strings.isNullOrEmpty(outerId)){
                    log.error("current order is assign shop,but shop out id is null");
                    throw new ServiceException("assign.shop.out.id.invalid");
                }
                String companyId = richSkusByShop.getExtra().get("assignShopCompanyId");
                if(Strings.isNullOrEmpty(companyId)){
                    log.error("current order is assign shop,but shop company id is null");
                    throw new ServiceException("assign.shop.company.id.invalid");
                }
                Response<Optional<Shop>> shopResponse =  psShopReadService.findByOuterIdAndBusinessId(outerId,Long.valueOf(companyId));
                if(!shopResponse.isSuccess()){
                    log.error("find shop by outer id:{} and business id:{} fail,error:{}",outerId,companyId,shopResponse.getError());
                    throw new ServiceException(shopResponse.getError());
                }
                Optional<Shop> shopOptional = shopResponse.getResult();
                if(!shopOptional.isPresent()){
                    log.error("not find shop by outer id:{} and business id:{}",outerId,companyId);
                    throw new ServiceException("assign.shop.not.exst");
                }
                Shop shop = shopOptional.get();
                tempExtra.put(TradeConstants.ASSIGN_SHOP_ID, shop.getId().toString());
                tempExtra.put(TradeConstants.IS_SINCE,richSkusByShop.getExtra().get(TradeConstants.IS_SINCE));
            }
            richSkusByShop.setExtra(tempExtra);
        }

        Map<String, String> shopOrderExtra = richSkusByShop.getExtra() == null ? Maps.newHashMap() : richSkusByShop.getExtra();
        shopOrderExtra.put(TradeConstants.ECP_ORDER_STATUS, String.valueOf(EcpOrderStatus.WAIT_SHIP.getValue()));

        //添加绩效店铺编码,通过openClient获取
        shopOrderExtra.put(TradeConstants.ERP_PERFORMANCE_SHOP_CODE,openClientFullOrder.getPerformanceShopCode());

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
                    item.setId(giftItem.getSpuId()==null?0L:giftItem.getSpuId());
                    item.setName(StringUtils.isEmpty(giftItem.getItemName())?"":giftItem.getItemName());
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

        Long invoiceId = null;
        if (Arguments.notNull(openClientFullOrder.getInvoice()) && Arguments.notNull(openClientFullOrder.getInvoice().getType())) {
            //生成发票信息(可能生成失败)
            invoiceId = this.addInvoice(openClientFullOrder.getInvoice());
        }

        richSkusByShop.setInvoiceId(invoiceId);
        //订单平台出资金额分摊到子订单上
        this.calculatePlatformDiscountForSkus(richSkusByShop);

        return richOrder;


    }

    private Long addInvoice(OpenClientOrderInvoice openClientOrderInvoice) {
        try {
            //获取发票类型
            Integer invoiceType = Integer.valueOf(openClientOrderInvoice.getType());
            if (Arguments.isNull(invoiceType)) {
                return null;
            }
            //获取detail
            Map<String, String> detail = openClientOrderInvoice.getDetail();
            if (detail == null) {
                detail = Maps.newHashMap();
            } else {
                detail.put("type", String.valueOf(invoiceType));
            }

            Invoice newInvoice = new Invoice();
            if (Objects.equals(invoiceType, 2)) {
                //2，增值税发票
                //公司
                detail.put("titleType", "2");
                //获取抬头(公司名称吧)
                String title = openClientOrderInvoice.getDetail().get("companyName");
                if (Arguments.isNull(title)){
                    title="";
                }
                newInvoice.setTitle(title);
            }else if(Objects.equals(invoiceType, 3)||Objects.equals(invoiceType, 1)){
                //3，电子发票
                String titleType= detail.get("titleType");
                if (Objects.equals(titleType,"1")){
                    //个人电子发票
                    newInvoice.setTitle("个人");
                }else if (Objects.equals(titleType,"2")){
                    //公司 电子发票
                    String title = openClientOrderInvoice.getDetail().get("companyName");
                    if (Arguments.isNull(title)){
                        title="";
                    }
                    newInvoice.setTitle(title);
                }
            }else {
                newInvoice.setTitle("其他");
            }
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
            log.error("create invoice failed,caused by {}", Throwables.getStackTraceAsString(e));
        }
        return null;
    }

    @Override
    protected void saveParanaOrder(RichOrder richOrder) {
        RichSkusByShop orginRichSkusByShop = richOrder.getRichSkusByShops().get(0);
        if (Objects.equals(orginRichSkusByShop.getOrderStatus(),OpenClientOrderStatus.PAID.getValue())
                || ((Objects.equals(MiddleChannel.JD.getValue(), orginRichSkusByShop.getOutFrom()))
                    && Objects.equals(orginRichSkusByShop.getOrderStatus(),OpenClientOrderStatus.NOT_PAID.getValue()))){
            //super.saveParanaOrder(richOrder);
            //重新实现父类saveParanaOrder逻辑，将eventBus修改为定时任务批处理方式
            if (richOrder == null) {
                return;
            }
            try {
                if (Arguments.notNull(orginRichSkusByShop.getReceiverInfo().getExtra()) && orginRichSkusByShop.getReceiverInfo().getExtra().containsKey(TradeConstants.VAUGE_ADDRESS)) {
                    for (RichSkusByShop order : richOrder.getRichSkusByShops()) {
                        order.getExtra().put(TradeConstants.VAUGE_ADDRESS, richOrder.getReceiverInfo().getExtra().get(TradeConstants.VAUGE_ADDRESS));
                    }
                }

            } catch (Exception e){
                log.error("set address flag error for richorder:{} cause:{}",richOrder,Throwables.getStackTraceAsString(e));

            }

            Response<List<Long>> createR = orderWriteService.create(richOrder);
            if (!createR.isSuccess()) {
                log.error("fail to save order:{},cause:{}", richOrder, createR.getError());
                return;
            }

            for (Long shopOrderId : createR.getResult()) {
                Response<ShopOrder> r = shopOrderReadService.findById(shopOrderId);
                if (!r.isSuccess()){
                    log.error("find shop order failed,shop order id is {},caused by {}",shopOrderId,r.getError());
                }else{
                    ShopOrder shopOrder = r.getResult();

//                    redisHandler.saveOrderId(shopOrder);
                    //只有非淘宝的订单可以抛出事件
                    if (!Objects.equals(shopOrder.getOutFrom(),"taobao")&&!Objects.equals(shopOrder.getOutFrom(),"tfenxiao")){
                        //eventBus.post(new OpenClientOrderSyncEvent(shopOrderId));
                        //eventBus存在队列阻塞和数据丢失风险，改通过定时任务执行的方式
                        this.createShipmentResultTask(shopOrder.getId());
                    }
                }
            }

            for (RichSkusByShop richSkusByShop : richOrder.getRichSkusByShops()) {
                // 天猫订单或天猫分销订单 转成Biz 异步化。避免脱敏耗时过长导致订单保存阻塞
                if (OpenClientChannel.from(richSkusByShop.getOutFrom()) == OpenClientChannel.TAOBAO
                    || OpenClientChannel.from(richSkusByShop.getOutFrom()) == OpenClientChannel.TFENXIAO) {
                    ReceiverInfoDecryptDTO dto = ReceiverInfoDecryptDTO.builder()
                        .shopId(richSkusByShop.getShop().getId())
                        .outId(richSkusByShop.getOuterOrderId())
                        .outFrom(richSkusByShop.getOutFrom())
                        .build();
                    createReceiverInfoDecryptTask(dto);
                }
            }
        }
    }

    /**
     * @Description TODO
     * @Date        2018/5/31
     * @param       shopOrderId
     * @return
     */
    private void createShipmentResultTask(Long shopOrderId){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.THIRD_ORDER_CREATE_SHIP.toString());
        biz.setContext(mapper.toJson(shopOrderId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 预售订单支付尾款之后生成预售订单后续处理流程任务
     * @param shopOrderId
     */
    private void createStepOrderrNotifyErpTask(Long shopOrderId,OpenClientFullOrder openClientFullOrder){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.STEP_ORDER_NOTIFY_ERP.toString());
        biz.setBizId(String.valueOf(shopOrderId));
        biz.setContext(JsonMapper.nonDefaultMapper().toJson(openClientFullOrder));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 收货人地址组装
     * @param consignee
     * @return
     *
     */
    @Override
    protected ReceiverInfo toReceiverInfo(OpenClientOrderConsignee consignee) {
        ReceiverInfo receiverInfo = super.toReceiverInfo(consignee);
        receiverInfoCompleter.complete(receiverInfo);
        return receiverInfo;
    }

    /**
     * 天猫订单请求聚石塔
     * @param richSkusByShop
     */
    private void syncReceiverInfo(RichSkusByShop richSkusByShop) {
        try {
            terminusErpOpenApiClient.doPost("sync.taobao.order.recever.info.api",
                    ImmutableMap.of("shopId", richSkusByShop.getShop().getId(), "orderId", richSkusByShop.getOuterOrderId(),"redirectUrl",poushengPagodaCommonRedirectUrl));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                    richSkusByShop.getOuterOrderId(), richSkusByShop.getShop().getId(), Throwables.getStackTraceAsString(e));
        }

    }

    /**
     * 天猫分销订单请求聚石塔
     * @param richSkusByShop 订单信息
     */
    private void syncFenxiaoReceiverInfo(RichSkusByShop richSkusByShop){
        try {
            if (log.isDebugEnabled()){
                log.debug("sync fenxiao receiver info start,shopId {},orderId {}",richSkusByShop.getShop().getId(),richSkusByShop.getOuterOrderId());
            }
           /* erpOpenApiClient.doPost("fenxiao.order.receiver.sync",
                    ImmutableMap.of("shopId", richSkusByShop.getShop().getId(), "orderId", richSkusByShop.getOuterOrderId()));*/
           terminusErpOpenApiClient.doPost("sync.taobao.fenxiao.order.recever.info.api",
                    ImmutableMap.of("shopId", richSkusByShop.getShop().getId(), "orderId", richSkusByShop.getOuterOrderId(),"redirectUrl",poushengPagodaFenxiaoRedirectUrl));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                    richSkusByShop.getOuterOrderId(), richSkusByShop.getShop().getId(), Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 平台级别优惠分摊到子订单上面
     * @param richSkusByShop 店铺订单
     */
    private void calculatePlatformDiscountForSkus(RichSkusByShop richSkusByShop){
        Map<String,String> extra = richSkusByShop.getExtra();
        String platformDiscount = extra.get(TradeConstants.PLATFORM_DISCOUNT_FOR_SHOP);
        if (!StringUtils.isEmpty(platformDiscount)){
            Map<String,Long> skuIdAndShareDiscount = Maps.newHashMap();
            List<RichSku> richSkus = richSkusByShop.getRichSkus();
            Long fees = 0L;
            for (RichSku richSku:richSkus){
                fees += richSku.getFee();
            }
            Long alreadyShareDiscout = 0L;
            for (int i = 0;i<richSkus.size()-1;i++){
                Long itemShareDiscount=0L;
                if (fees>0){
                    itemShareDiscount =  (richSkus.get(i).getFee()*Long.valueOf(platformDiscount))/fees;
                }
                alreadyShareDiscout += itemShareDiscount;
                skuIdAndShareDiscount.put(richSkus.get(i).getOuterSkuId(),itemShareDiscount);
            }
            //计算剩余没有分配的平台优惠
            Long remainShareDiscount = Long.valueOf(platformDiscount)-alreadyShareDiscout;
            for (RichSku richSku:richSkus){
                Long shareDiscount = skuIdAndShareDiscount.get(richSku.getOuterSkuId());
                Map<String, String> skuExtra = richSku.getExtra();
                //子订单插入平台优惠金额
                if (shareDiscount==null){
                    skuExtra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SKU, String.valueOf(remainShareDiscount));
                }else{
                    skuExtra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SKU, String.valueOf(shareDiscount));
                }
                richSku.setExtra(skuExtra);
            }
        }

    }

    /**
     * 创建收货人信息脱敏biz任务
     * @param dto
     */
    private void createReceiverInfoDecryptTask(ReceiverInfoDecryptDTO dto){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.TMALL_RECEIVER_INFO_DECRYPT.toString());
        biz.setContext(mapper.toJson(dto));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 天猫订单请求聚石塔
     * @param dto
     */
    public void syncReceiverInfo(ReceiverInfoDecryptDTO dto) {
        try {
            terminusErpOpenApiClient.doPost("sync.taobao.order.recever.info.api",
                ImmutableMap.of("shopId", dto.getShopId(), "orderId", dto.getOutId(),"redirectUrl",poushengPagodaCommonRedirectUrl));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                dto.getOutId(), dto.getShopId(), Throwables.getStackTraceAsString(e));
        }

    }

    /**
     * 天猫分销订单请求聚石塔
     * @param dto 订单信息
     */
    public void syncFenxiaoReceiverInfo(ReceiverInfoDecryptDTO dto){
        try {
            if (log.isDebugEnabled()){
                log.debug("sync fenxiao receiver info start,shopId {},orderId {}",dto.getShopId(),dto.getOutId());
            }
            terminusErpOpenApiClient.doPost("sync.taobao.fenxiao.order.recever.info.api",
                ImmutableMap.of("shopId", dto.getShopId(), "orderId", dto.getOutId(),"redirectUrl",poushengPagodaFenxiaoRedirectUrl));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                dto.getOutId(), dto.getShopId(), Throwables.getStackTraceAsString(e));
        }
    }
}
