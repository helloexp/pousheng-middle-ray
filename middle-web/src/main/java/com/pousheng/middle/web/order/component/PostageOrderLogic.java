package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.OrderNoteProcessingFlag;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.dto.fsm.MiddleOrderType;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.enums.ShipmentDispatchType;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 补拍邮费商品订单逻辑
 *
 * @author tanlongjun
 */
@Slf4j
@Component
public class PostageOrderLogic {

    @Value("${postage.sku.code}")
    public String postageSkuCode;

    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();

    @Autowired
    private ShipmentWriteManger shipmentWriteManger;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @RpcConsumer
    private PsShopReadService psShopReadService;

    @Autowired
    private WarehouseClient warehouseClient;

    @Autowired
    private ShopCacher shopCacher;

    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @Autowired
    private MiddleShopCacher middleShopCacher;

    /**
     * 移除补拍邮费商品的库存变化
     *
     * @param list
     */
    public void removePostageSkuCode(List<InventoryChangeDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.removeIf(dto -> postageSkuCode.equals(dto.getSkuCode()));
    }

    /**
     * 处理补差邮费商品
     * 1.若订单中只有邮费商品
     *
     * @param richOrder
     */
    public void handlePostageOrder(RichOrder richOrder) {
        if (Objects.isNull(richOrder)
            || CollectionUtils.isEmpty(richOrder.getRichSkusByShops())) {
            return;
        }

        for (RichSkusByShop richSkusByShop : richOrder.getRichSkusByShops()) {
            int count = richSkusByShop.getRichSkus().size();
            if (count != 1) {
                continue;
            }
            RichSku richSku = richSkusByShop.getRichSkus().get(0);
            if (postageSkuCode.equals(richSku.getSku().getSkuCode())) {
                //标识邮费订单类型
                richSkusByShop.setOrderType(MiddleOrderType.POSTAGE.getValue());
            }
        }
    }

    /**
     * 调整邮费及订单金额
     *
     * @param richSkusByShop
     * @param richSku
     */
    @Deprecated
    protected void handlePostageItemFee(RichSkusByShop richSkusByShop, RichSku richSku) {
        //补差邮费总费用=邮费商品费用
        int postageFee = richSku.getFee().intValue();
        // 订单金额去掉商品金额
        richSkusByShop.setFee(richSkusByShop.getFee() - postageFee);
        // 邮费中加上补差邮费商品
        richSkusByShop.setShipFee(richSkusByShop.getShipFee() + postageFee);
    }

    /**
     * 创建邮费订单的发货单
     *
     * @param shopOrder
     */
    public void createPostageShipment(ShopOrder shopOrder) {
        if (Objects.isNull(shopOrder.getType())
            || MiddleOrderType.POSTAGE.getValue() != shopOrder.getType()) {
            return;
        }
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
            MiddleOrderStatus.WAIT_HANDLE.getValue());
        if (skuOrders.size() != 1) {
            return;
        }
        SkuOrder skuOrder = skuOrders.get(0);

        //获取发货店铺的id
        long deliveyShopId = shopOrder.getShopId();
        //获取skuOid,quantity的集合
        Map<Long, Integer> skuOrderIdAndQuantity = Maps.newHashMap();
        skuOrderIdAndQuantity.put(skuOrder.getId(), skuOrder.getQuantity());

        //获取该发货单中涉及到的sku订单
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        //封装发货信息
        List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrder, skuOrderIdAndQuantity, deliveyShopId, shopOrder);
        //发货单商品金额
        Long shipmentItemFee = 0L;
        //发货单总的优惠
        Long shipmentDiscountFee = 0L;
        //发货单总的净价
        Long shipmentTotalFee = 0L;
        //运费
        Long shipmentShipFee = 0L;
        //运费优惠
        Long shipmentShipDiscountFee = 0L;

        shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee() == null ? 0 : shopOrder.getOriginShipFee());
        shipmentShipDiscountFee = shipmentShipFee - Long.valueOf(
            shopOrder.getShipFee() == null ? 0 : shopOrder.getShipFee());

        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice() * shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = shipmentItem.getSkuDiscount() + shipmentDiscountFee;
            shipmentTotalFee = shipmentItem.getCleanFee() + shipmentTotalFee;
        }
        //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
        Long shipmentTotalPrice = shipmentTotalFee + shipmentShipFee - shipmentShipDiscountFee;

        Shipment shipment = makePostageShipment(shopOrder, deliveyShopId, shopOrder.getShopName(), shipmentItemFee,
            shipmentDiscountFee, shipmentTotalFee, shipmentShipFee, shipmentShipDiscountFee,
            shipmentTotalPrice, shopOrder.getShopId());
        shipment.setSkuInfos(skuOrderIdAndQuantity);
        //存在待处理的备注订单需要生成的是占库发货单
        Map<String, String> shopOrderExtra = shopOrder.getExtra();
        if (StringUtils.isNotEmpty(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG))
            && Objects.equals(shopOrderExtra.get(TradeConstants.ORDER_NOTE_PROCESS_FLAG),
            OrderNoteProcessingFlag.WAIT_HANLE.name())) {
            shipment.setIsOccupyShipment(ShipmentOccupyType.SALE_Y.name());
        }
        shipment.setType(ShipmentType.SALES_SHIP.value());
        shipment.setShopId(shopOrder.getShopId());
        shipment.setShopName(shopOrder.getShopName());
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
        shipment.setExtra(extraMap);

        //自动派单
        shipment.setDispatchType(ShipmentDispatchType.AUTO.value());
        Long shipmentId = null;
        //创建发货单
        try {
            shipmentId = shipmentWriteManger.createPostageShipment(shipment, shopOrder, skuOrders);
        } catch (Exception e) {
            log.error("create postage shipment fail for shop order(id:{}) and lock stock fail,cause:{}",
                shopOrder.getOrderCode(),
                Throwables.getStackTraceAsString(e));
            throw new ServiceException(e.getMessage());
        }

        if (!Objects.isNull(shipmentId)) {
            //生成发货单同步恒康生成pos的任务
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setBizId(String.valueOf(shipment.getId()));
            biz.setBizType(PoushengCompensateBizType.SYNC_ORDER_POS_TO_HK.name());
            biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
        }

    }

    /**
     * 组装发货单参数
     *
     * @param shopOrder     店铺订单
     * @param deliverShopId 接单店铺id
     * @return 返回组装的发货单
     */
    private Shipment makePostageShipment(ShopOrder shopOrder, Long deliverShopId, String deliverShopName,
                                         Long shipmentItemFee, Long shipmentDiscountFee,
                                         Long shipmentTotalFee, Long shipmentShipFee, Long shipmentShipDiscountFee,
                                         Long shipmentTotalPrice, Long shopId) {
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.SHIPPED.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrder.getId(), OrderLevel.SHOP));
        shipment.setShipWay(Integer.parseInt(TradeConstants.MPOS_SHOP_DELIVER));

        //是否必须发货 店发默认为否
        shipment.setMustShip(0);

        Map<String, String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();

        //下单店铺代码
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        String shopCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE,
            openShop);
        String shopName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME,
            openShop);
        String shopOutCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE,
            openShop);
        //店发设置仓库对应的店铺id
        Long shipId = deliverShopId;
        //绩效店铺为空，默认当前店铺为绩效店铺
        defaultPerformanceShop(openShop, shopCode, shopName, shopOutCode);
        shipmentExtra.setShipmentWay(TradeConstants.MPOS_SHOP_DELIVER);
        shipmentExtra.setWarehouseId(shipId);
        shipmentExtra.setWarehouseName(shopName);
        shipmentExtra.setTakeWay(shopOrder.getExtra().get(TradeConstants.IS_SINCE));
        shipmentExtra.setIsAppint(shopOrder.getExtra().get(TradeConstants.IS_ASSIGN_SHOP));
        shipmentExtra.setErpOrderShopCode(shopCode);
        shipmentExtra.setErpOrderShopName(shopName);
        shipmentExtra.setErpPerformanceShopCode(shopCode);
        shipmentExtra.setErpPerformanceShopName(shopName);
        shipmentExtra.setIsPostageOrder(true);
        shipmentExtra.setShipmentDate(new Date());
        shipment.setShipId(shipId);

        shipmentExtra.setShipmentItemFee(shipmentItemFee);
        //发货单运费金额
        shipmentExtra.setShipmentShipFee(shipmentShipFee);
        //发货单优惠金额
        shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
        //发货单总的净价
        shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
        shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
        //添加物流编码
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
            && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        } else {
            shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
        }
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));

        shipment.setExtra(extraMap);

        return shipment;
    }

    /**
     * 查找收货人信息
     *
     * @param orderId    订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的json串
     */
    private String findReceiverInfos(Long orderId, OrderLevel orderLevel) {

        List<ReceiverInfo> receiverInfos = doFindReceiverInfos(orderId, orderLevel);

        if (org.springframework.util.CollectionUtils.isEmpty(receiverInfos)) {
            log.error("receiverInfo not found where orderId={}", orderId);
            throw new JsonResponseException("receiver.info.not.found");
        }

        ReceiverInfo receiverInfo = receiverInfos.get(0);

        try {
            return OBJECT_MAPPER.writeValueAsString(receiverInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 查找收货人信息
     *
     * @param orderId    订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的list集合
     */
    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }

    /**
     * 发货单中填充sku订单信息
     *
     * @param skuOrder              子单集合
     * @param skuOrderIdAndQuantity 子单的主键和数量的集合
     * @return shipmentItem的集合
     */
    public List<ShipmentItem> makeShipmentItems(SkuOrder skuOrder, Map<Long, Integer> skuOrderIdAndQuantity,
                                                Long warehouseId, ShopOrder shopOrder) {
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuOrderIdAndQuantity.size());
        ShipmentItem shipmentItem = new ShipmentItem();
        if (skuOrder.getShipmentType() != null && Objects.equals(skuOrder.getShipmentType(), 1)) {
            shipmentItem.setIsGift(Boolean.TRUE);
        } else {
            shipmentItem.setIsGift(Boolean.FALSE);
        }
        // 不占库
        shipmentItem.setCareStock(0);
        shipmentItem.setWarehouseId(warehouseId);
        shipmentItem.setShopId(shopOrder.getShopId());
        //已发货
        shipmentItem.setStatus(MiddleShipmentsStatus.SHIPPED.getValue());
        shipmentItem.setQuantity(skuOrder.getQuantity());
        shipmentItem.setRefundQuantity(0);
        // 实际发货数量
        shipmentItem.setShipQuantity(skuOrder.getQuantity());
        shipmentItem.setSkuOrderId(skuOrder.getId());
        shipmentItem.setSkuName(skuOrder.getItemName());
        shipmentItem.setSkuOutId(skuOrder.getOutId());
        shipmentItem.setSkuPrice(Math.round(skuOrder.getOriginFee() / shipmentItem.getQuantity()));
        //目前是子单整单发货，所以不需要分摊平台优惠金额
        shipmentItem.setSharePlatformDiscount(0);
        //积分
        shipmentItem.setIntegral(0);
        Long disCount = skuOrder.getDiscount() + Long.valueOf(this.getShareDiscount(skuOrder));
        shipmentItem.setSkuDiscount(
            this.getDiscount(skuOrder.getQuantity(), skuOrder.getQuantity(), Math.toIntExact(disCount)));
        shipmentItem.setCleanFee(
            this.getCleanFee(shipmentItem.getSkuPrice(), shipmentItem.getSkuDiscount(), shipmentItem.getQuantity()));
        shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(), shipmentItem.getQuantity()));
        shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
        shipmentItem.setSkuCode(skuOrder.getSkuCode());
        //商品id
        String outItemId = "";
        try {
            //商品属性
            shipmentItem.setExtraJson(JSON_MAPPER.toJson(skuOrder.getSkuAttrs()));
            outItemId = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID, skuOrder);
        } catch (Exception e) {
            log.info("outItemmId is not exist", e);
        }
        shipmentItem.setItemId(outItemId);
        shipmentItems.add(shipmentItem);
        return shipmentItems;
    }

    private String getShareDiscount(SkuOrder skuOrder) {
        String skuShareDiscount = "";
        try {
            skuShareDiscount = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(),
                TradeConstants.SKU_SHARE_DISCOUNT);
        }
        return StringUtils.isEmpty(skuShareDiscount) ? "0" : skuShareDiscount;
    }

    /**
     * 计算商品净价
     *
     * @param cleanFee        商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品净价
     */
    private Integer getCleanPrice(Integer cleanFee, Integer shipSkuQuantity) {
        return Math.round(Long.valueOf(cleanFee) / Long.valueOf(shipSkuQuantity));
    }

    /**
     * @param skuQuantity     sku订单中商品的数量
     * @param shipSkuQuantity 发货的sku商品的数量
     * @param skuDiscount     sku订单中商品的折扣
     * @return 返回四舍五入的计算结果, 得到发货单中的sku商品的折扣
     */
    private Integer getDiscount(Integer skuQuantity, Integer shipSkuQuantity, Integer skuDiscount) {
        return Math.round(Long.valueOf(skuDiscount) * Long.valueOf(shipSkuQuantity) / Long.valueOf(skuQuantity));
    }

    /**
     * 计算总净价
     *
     * @param skuPrice        商品原价
     * @param discount        发货单中sku商品的折扣
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品总的净价
     */
    private Integer getCleanFee(Integer skuPrice, Integer discount, Integer shipSkuQuantity) {

        return skuPrice * shipSkuQuantity - discount;
    }

    /**
     * 店铺没有设置虚拟店，默认为当前店铺
     *
     * @param openShop
     * @param shopCode
     * @param shopName
     */
    public Shop defaultPerformanceShop(OpenShop openShop, String shopCode, String shopName, String shopOutCode) {
        if (Arguments.isNull(shopCode)) {
            try {
                String appKey = openShop.getAppKey();
                String outerId = appKey.substring(appKey.indexOf("-") + 1);
                String companyId = appKey.substring(0, appKey.indexOf("-"));
                Response<com.google.common.base.Optional<Shop>> optionalRes = psShopReadService
                    .findByOuterIdAndBusinessId(
                        outerId, Long.valueOf(companyId));
                if (!optionalRes.isSuccess()) {
                    log.error("find shop by outer id:{} business id:{} fail,error:{}", outerId, companyId,
                        optionalRes.getError());
                    return null;
                }

                com.google.common.base.Optional<Shop> shopOptional = optionalRes.getResult();
                if (!shopOptional.isPresent()) {
                    log.error("not find shop by outer id:{} business id:{} ", outerId, companyId);
                    return null;
                }
                Shop shop = shopOptional.get();
                shopName = shop.getName();
                ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                shopCode = shopExtraInfo.getShopInnerCode();
                shopOutCode = shop.getOuterId();
                return shop;
            } catch (Exception e) {
                log.error("find member shop(openId:{}) failed", openShop.getId(), e);
            }
        }
        return null;
    }
}
