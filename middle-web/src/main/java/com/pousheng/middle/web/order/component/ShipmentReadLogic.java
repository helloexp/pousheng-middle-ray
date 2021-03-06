package com.pousheng.middle.web.order.component;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.impl.service.PsShipmentItemReadServiceImpl;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.utils.SkuCodeUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentItemReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class ShipmentReadLogic {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @RpcConsumer
    private ShipmentItemReadService shipmentItemReadService;
    @RpcConsumer
    private PsShipmentItemReadServiceImpl psShipmentItemReadService;
    @Autowired
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;
    @Autowired
    private ShopReadService shopReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;

    /**
     * JIT店铺编号
     */
    @Value("${jit.open.shop.id}")
    private String shopId;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 发货单详情
     */
    public ShipmentDetail orderDetail(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);
        OrderShipment orderShipment = findOrderShipmentByShipmentId(shipmentId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());

        ShipmentDetail shipmentDetail = new ShipmentDetail();
        shipmentDetail.setOrderShipment(orderShipment);
        shipmentDetail.setShipment(shipment);
        shipmentDetail.setShopOrder(shopOrder);
        shipmentDetail.setShipmentItems(getShipmentItems(shipment));
        ShipmentExtra shipmentExtra = getShipmentExtra(shipment);
        setShopTelInfo(shipment, shipmentExtra);
        shipmentDetail.setShipmentExtra(shipmentExtra);
        setInvoiceInfo(shipmentDetail, orderShipment.getOrderId());
        setReceiverInfo(shipmentDetail, shipment);
        List<Payment> payments = orderReadLogic.findOrderPaymentInfo(orderShipment.getOrderId());
        if (!CollectionUtils.isEmpty(payments)) {
            shipmentDetail.setPayment(payments.get(0));
        }

        return shipmentDetail;

    }


    public Response<ShipmentPreview> orderShipPreview(Long shopOrderId, String data) {
        Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);

        Response<OrderDetail> orderDetailRes = orderReadLogic.orderDetail(shopOrderId);
        if (!orderDetailRes.isSuccess()) {
            log.error("find order detail by order id:{} fail,error:{}", shopOrderId, orderDetailRes.getError());
            throw new JsonResponseException(orderDetailRes.getError());
        }
        OrderDetail orderDetail = orderDetailRes.getResult();
        List<SkuOrder> allSkuOrders = orderDetail.getSkuOrders();
        List<SkuOrder> currentSkuOrders = allSkuOrders.stream().filter(skuOrder -> skuOrderIdAndQuantity.containsKey(skuOrder.getId())).collect(Collectors.toList());
        currentSkuOrders.forEach(skuOrder -> skuOrder.setQuantity(skuOrderIdAndQuantity.get(skuOrder.getId())));


        //封装发货预览基本信息
        ShipmentPreview shipmentPreview = new ShipmentPreview();
        shipmentPreview.setInvoices(orderDetail.getInvoices());
        shipmentPreview.setPayment(orderDetail.getPayment());
        List<OrderReceiverInfo> orderReceiverInfos = orderDetail.getOrderReceiverInfos();
        shipmentPreview.setReceiverInfo(JsonMapper.nonDefaultMapper().fromJson(orderReceiverInfos.get(0).getReceiverInfoJson(), ReceiverInfo.class));
        shipmentPreview.setShopOrder(orderDetail.getShopOrder());
        shipmentPreview.setShopId(orderDetail.getShopOrder().getShopId());
        ShopOrder shopOrder = orderDetail.getShopOrder();
        Map<String, String> shopOrderMap = shopOrder.getExtra();
        String expressCode = shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE);
        if (!org.springframework.util.StringUtils.isEmpty(expressCode)) {
            shipmentPreview.setOrderHkExpressCode(expressCode);
        }
        String expressName = shopOrderMap.get(TradeConstants.SHOP_ORDER_HK_EXPRESS_NAME);
        if (!org.springframework.util.StringUtils.isEmpty(expressName)) {
            shipmentPreview.setOrderHkExpressName(expressName);
        }
        //封装发货预览商品信息
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithCapacity(currentSkuOrders.size());
        for (SkuOrder skuOrder : currentSkuOrders) {
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setSkuOrderId(skuOrder.getId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setQuantity(skuOrder.getQuantity());
            if (skuOrder.getShipmentType() != null && Objects.equals(skuOrder.getShipmentType(), 1)) {
                shipmentItem.setIsGift(Boolean.TRUE);
            } else {
                shipmentItem.setIsGift(Boolean.FALSE);
            }
            SkuOrder originSkuOrder = (SkuOrder) orderReadLogic.findOrder(skuOrder.getId(), OrderLevel.SKU);
            //积分
            String originIntegral = "";
            try {
                originIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL, skuOrder);
            } catch (JsonResponseException e) {
                log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_INTEGRAL);
            }
            Integer integral = StringUtils.isEmpty(originIntegral) ? 0 : Integer.valueOf(originIntegral);
            shipmentItem.setIntegral(this.getIntegral(integral, originSkuOrder.getQuantity(), skuOrder.getQuantity()));
            //获取商品原价
            shipmentItem.setSkuPrice(Integer.valueOf(Math.round(originSkuOrder.getOriginFee() / originSkuOrder.getQuantity())));
            //查看生成发货单的sku商品折扣
            Long disCount = skuOrder.getDiscount() + Long.valueOf(this.getShareDiscount(skuOrder));
            shipmentItem.setSkuDiscount(this.getDiscount(originSkuOrder.getQuantity(), skuOrder.getQuantity(), Math.toIntExact(disCount)));
            //查看sku商品的总的净价
            shipmentItem.setCleanFee(this.getCleanFee(shipmentItem.getSkuPrice(), shipmentItem.getSkuDiscount(), shipmentItem.getQuantity()));
            //查看sku商品净价
            shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(), shipmentItem.getQuantity()));
            //商品id--itemId
            //商品id
            String outItemId = "";
            try {
                //商品属性
                shipmentItem.setExtraJson(mapper.toJson(skuOrder.getSkuAttrs()));
                outItemId = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID, skuOrder);
            } catch (Exception e) {
                log.info("outItemmId is not exist");
            }
            shipmentItem.setItemId(outItemId);

            shipmentItems.add(shipmentItem);
        }
        shipmentPreview.setShipmentItems(shipmentItems);

        return Response.ok(shipmentPreview);
    }


    public Response<ShipmentPreview> changeShipPreview(Long refundId, String data) {
        Map<String, Integer> skuCodeAndQuantity = analysisSkuCodeAndQuantity(data);
        Refund refund = refundReadLogic.findRefundById(refundId);
        //判断是丢件补发类型的售后单还是换货售后单
        List<RefundItem> originRefundChangeItems = null;
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            originRefundChangeItems = refundReadLogic.findRefundChangeItems(refund);
        } else {
            originRefundChangeItems = refundReadLogic.findRefundLostItems(refund);
        }
        //获取当前需要生成发货单的售后商品
        List<RefundItem> refundChangeItems = Lists.newArrayList();
        originRefundChangeItems.forEach(refundItem -> {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundChangeItems.add(refundItem);
            }
        });
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);

        //订单基本信息
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());//orderRefund.getOrderId()为交易订单id
        List<Invoice> invoices = orderReadLogic.findInvoiceInfo(orderRefund.getOrderId());
        List<Payment> payments = orderReadLogic.findOrderPaymentInfo(orderRefund.getOrderId());
        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(orderRefund.getOrderId());


        //封装发货预览基本信息
        ShipmentPreview shipmentPreview = new ShipmentPreview();
        shipmentPreview.setInvoices(invoices);
        if (!CollectionUtils.isEmpty(payments)) {
            shipmentPreview.setPayment(payments.get(0));
        }
        shipmentPreview.setReceiverInfo(receiverInfo);
        shipmentPreview.setShopOrder(shopOrder);
        shipmentPreview.setShopId(refund.getShopId());
        //封装发货预览商品信息
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithCapacity(refundChangeItems.size());
        for (RefundItem refundItem : refundChangeItems) {

            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setSkuCode(refundItem.getSkuCode());
            shipmentItem.setOutSkuCode(refundItem.getOutSkuCode());
            shipmentItem.setSkuName(refundItem.getSkuName());
            shipmentItem.setQuantity(skuCodeAndQuantity.get(refundItem.getSkuCode()));
            shipmentItem.setCleanPrice(refundItem.getCleanPrice());
            shipmentItem.setCleanFee(refundItem.getCleanFee());
            shipmentItem.setSkuPrice(refundItem.getSkuPrice());
            shipmentItem.setSkuDiscount(refundItem.getSkuDiscount());
            shipmentItem.setItemId(refundItem.getItemId());

            try {
                shipmentItem.setExtraJson(mapper.toJson(refundItem.getAttrs()));
            } catch (Exception e) {
                log.error("attrs json is error {}", refundItem.getAttrs());
            }

            shipmentItems.add(shipmentItem);
        }
        shipmentPreview.setShipmentItems(shipmentItems);
        //添加换货收货人信息
        shipmentPreview.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        
		// XXX RAY POUS925 POUS 1728 2019.09.09: 售后单指定快递生成发货单时，展示的快递信息未更新为指定的快递信息
		if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())
				|| Objects.equals(refund.getRefundType(),
						MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
			RefundExtra refundExt = refundReadLogic.findRefundExtra(refund);
			if (!Strings.isNullOrEmpty(refundExt.getNewShipmentCorpName())) {
				shipmentPreview.getShopOrder().getExtra().put("orderHkExpressName", refundExt.getNewShipmentCorpName());
			}
		}
        return Response.ok(shipmentPreview);
    }

    public List<OrderShipment> findByOrderIdAndType(Long orderId) {
        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find order shipment by order id:{} level:{} fail,error:{}", orderId, OrderLevel.SHOP.toString(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }

    private Map<Long, Integer> analysisSkuOrderIdAndQuantity(String data) {
        Map<Long, Integer> skuOrderIdAndQuantity = mapper.fromJson(data, mapper.createCollectionType(HashMap.class, Long.class, Integer.class));
        if (skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuOrderIdAndQuantity:{}", data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }

    private Map<String, Integer> analysisSkuCodeAndQuantity(String data) {
        Map<String, Integer> skuOrderIdAndQuantity = mapper.fromJson(data, mapper.createCollectionType(HashMap.class, String.class, Integer.class));
        if (skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuCodeAndQuantity:{}", data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }


    public List<OrderShipment> findByAfterOrderIdAndType(Long afterSaleOrderId) {
        Response<List<OrderShipment>> response = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find order shipment by order id:{} level:{} fail,error:{}", afterSaleOrderId, OrderLevel.SHOP.toString(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }


    public Shipment findShipmentById(Long shipmentId) {
        Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
        if (!shipmentRes.isSuccess()) {
            log.error("find shipment by id:{} fail,error:{}", shipmentId, shipmentRes.getError());
            throw new JsonResponseException(shipmentRes.getError());
        }
        return shipmentRes.getResult();
    }

    public Shipment findShipmentByShipmentCode(String shipmentCode) {
        Response<Shipment> shipmentResponse = shipmentReadService.findShipmentCode(shipmentCode);
        if (!shipmentResponse.isSuccess()) {
            log.error("find shipment by shipmentCode:{} fail,error:{}", shipmentCode, shipmentResponse.getError());
            throw new JsonResponseException(shipmentResponse.getError());
        }
        return shipmentResponse.getResult();
    }

    public OrderShipment findOrderShipmentById(Long orderShipmentId) {
        Response<OrderShipment> orderShipmentRes = orderShipmentReadService.findById(orderShipmentId);
        if (!orderShipmentRes.isSuccess()) {
            log.error("find order shipment by id:{} fail,error:{}", orderShipmentId, orderShipmentRes.getError());
            throw new JsonResponseException(orderShipmentRes.getError());
        }

        return orderShipmentRes.getResult();
    }

    public OrderShipment findOrderShipmentByShipmentId(Long shipmenId) {
        Response<OrderShipment> orderShipmentRes = orderShipmentReadService.findByShipmentId(shipmenId);
        if (!orderShipmentRes.isSuccess()) {
            log.error("find order shipment by shipment id:{} fail,error:{}", shipmenId, orderShipmentRes.getError());
            throw new JsonResponseException(orderShipmentRes.getError());
        }

        return orderShipmentRes.getResult();
    }


    public OrderShipment findOrderShipmentByShipmentCode(String shipmentCode) {
        Response<Shipment> shipmentResponse = shipmentReadService.findShipmentCode(shipmentCode);
        if (!shipmentResponse.isSuccess()) {
            log.error("find  shipment by shipment code:{} fail,error:{}", shipmentCode, shipmentResponse.getError());
            throw new JsonResponseException(shipmentResponse.getError());
        }
        Response<OrderShipment> orderShipmentRes = orderShipmentReadService.findByShipmentId(shipmentResponse.getResult().getId());
        if (!orderShipmentRes.isSuccess()) {
            log.error("find order shipment by shipment id:{} fail,error:{}", shipmentResponse.getResult().getId(), orderShipmentRes.getError());
            throw new JsonResponseException(orderShipmentRes.getError());
        }

        return orderShipmentRes.getResult();
    }


    /**
     * 根据仓库编码查询公司规则
     *
     * @param warehouseCode
     * @return
     */
    public Response<WarehouseCompanyRule> findCompanyRuleByWarehouseCode(String warehouseCode) {

        String companyCode;
        try {
            //获取公司编码
            companyCode = Splitter.on("-").splitToList(warehouseCode).get(0);
        } catch (Exception e) {
            log.error("analysis warehouse code:{} fail,cause:{}", warehouseCode, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("analysis.warehouse.code.fail");
        }

        Response<WarehouseCompanyRule> ruleRes = warehouseCompanyRuleReadService.findByCompanyCode(companyCode);
        if (!ruleRes.isSuccess()) {
            log.error("find warehouse company rule by company code:{} fail,error:{}", companyCode, ruleRes.getError());
            throw new JsonResponseException(ruleRes.getError());
        }

        WarehouseCompanyRule companyRule = ruleRes.getResult();
        if (Arguments.isNull(companyRule)) {
            log.error("not find warehouse company rule by company code:{}", companyCode);
            return Response.fail("warehouse.company.rule.not.exist");
        }
        return Response.ok(companyRule);
    }

    /**
     * 设置下单店铺，发货仓店铺手机号信息
     */
    private void setShopTelInfo(Shipment shipment, ShipmentExtra shipmentExtra) {
        //设置发货店仓手机号 1:店发，2：仓发
        if (Objects.equals(shipment.getShipWay(), 1)) {

            Shop shop = null;
            // 非补邮费订单 查询发货店铺信息
            if (!isPostageOrder(shipmentExtra)) {
                shop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
            }
            if (null != shop) {
                //设置店发的手机号,关联会员中心获取
                Optional<MemberShop> memberShopOptional = memberShopOperationLogic.findShopByCodeAndType(shop.getOuterId(), 1, shop.getBusinessId().toString());
                if (memberShopOptional.isPresent()) {
                    MemberShop memberShop = memberShopOptional.get();
                    shipmentExtra.setWarehouseTelephone(memberShop.getTelphone());
                }
            }
        }
        if (Objects.equals(shipment.getShipWay(), 2)) {
            WarehouseDTO warehouse = warehouseCacher.findById(shipmentExtra.getWarehouseId());
            if (null != warehouse) {
                //设置发货仓的手机号
                shipmentExtra.setWarehouseTelephone(warehouse.getExtra().get("telephone"));
            }
        }
        //设置订单来源店铺手机号
        OpenShop opshop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
        if (null != opshop) {
            String companyCode = opshop.getExtra().get("companyCode");
            String outshopCode = opshop.getExtra().get("hkPerformanceShopOutCode");
            if (!ObjectUtils.isEmpty(companyCode) && !ObjectUtils.isEmpty(outshopCode)) {
                //设置店发的手机号,关联会员中心获取
                Optional<MemberShop> memberShops = memberShopOperationLogic.findShopByCodeAndType(outshopCode, 1, companyCode);
                if (memberShops.isPresent()) {
                    MemberShop memberShop = memberShops.get();
                    shipmentExtra.setErpOrderShopTelephone(memberShop.getTelphone());
                }
            }
        }
    }


    /**
     * 商品详情返回发票信息
     */
    private void setInvoiceInfo(ShipmentDetail shipmentDetail, Long shopOrderId) {

        shipmentDetail.setInvoices(orderReadLogic.findInvoiceInfo(shopOrderId));
    }

    /**
     * 收货地址信息
     */
    private void setReceiverInfo(ShipmentDetail shipmentDetail, Shipment shipment) {
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(), ReceiverInfo.class);
        shipmentDetail.setReceiverInfo(receiverInfo);
    }


    public List<ShipmentItem> getShipmentItems(Shipment shipment) {
        Map<String, String> extraMap = shipment.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("shipment(id:{}) extra field is null", shipment.getId());
            throw new JsonResponseException("shipment.extra.is.null");
        }
        // 如果明细对应的key:shipmentItemInfo 有内容则直接取,
        // 否则根据发货单id去发货明细表取
        if (!extraMap.containsKey(TradeConstants.SHIPMENT_ITEM_INFO)) {
            log.error("shipment(id:{}) extra not contain key:{}", shipment.getId(), TradeConstants.SHIPMENT_ITEM_INFO);
            throw new JsonResponseException("shipment.extra.item.info.null");
        }

        ShipmentExtra shipmentExtra = this.getShipmentExtra(shipment);

        if (StringUtils.isEmpty(extraMap.get(TradeConstants.SHIPMENT_ITEM_INFO))) {
            List<ShipmentItem> list = findByShipmentId(shipment.getId());
            list.forEach(it ->  it.setItemWarehouseName(shipmentExtra.getWarehouseName()));
            return list;
        }

        List<ShipmentItem> list = mapper.fromJson(extraMap.get(TradeConstants.SHIPMENT_ITEM_INFO), mapper.createCollectionType(List.class, ShipmentItem.class));
        //添加发货仓库
        for (ShipmentItem shipmentItem : list) {
            shipmentItem.setWarehouseId(shipmentExtra.getWarehouseId());
            shipmentItem.setItemWarehouseName(shipmentExtra.getWarehouseName());
        }
        return list;
    }


    public ShipmentExtra getShipmentExtra(Shipment shipment) {
        Map<String, String> extraMap = shipment.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("shipment(id:{}) extra field is null", shipment.getId());
            throw new JsonResponseException("shipment.extra.is.null");
        }
        if (!extraMap.containsKey(TradeConstants.SHIPMENT_EXTRA_INFO)) {
            log.error("shipment(id:{}) extra not contain key:{}", shipment.getId(), TradeConstants.SHIPMENT_EXTRA_INFO);
            throw new JsonResponseException("shipment.extra.extra.info.null");
        }

        return mapper.fromJson(extraMap.get(TradeConstants.SHIPMENT_EXTRA_INFO), ShipmentExtra.class);


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
     * @return 总净价
     */
    private Integer getCleanFee(Integer skuPrice, Integer discount, Integer shipSkuQuantity) {

        return skuPrice * shipSkuQuantity - discount;
    }

    /**
     * 计算商品净价
     *
     * @param cleanFee        商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 商品净价
     */
    private Integer getCleanPrice(Integer cleanFee, Integer shipSkuQuantity) {
        return Math.round(cleanFee / shipSkuQuantity);
    }

    /**
     * 计算积分
     *
     * @param integral            sku订单获取的积分
     * @param skuQuantity         sku订单总的数量
     * @param shipmentSkuQuantity 发货单中该sku订单的数量
     * @return 获取发货单中sku订单的积分
     */
    private Integer getIntegral(Integer integral, Integer skuQuantity, Integer shipmentSkuQuantity) {
        return Math.round(integral * shipmentSkuQuantity / skuQuantity);
    }

    /**
     * 判断返货单是否已经计算过运费
     *
     * @param shopOrderId 店铺订单主键
     * @return true:已经计算过发货单,false:没有计算过发货单
     */
    public boolean isShipmentFeeCalculated(long shopOrderId) {
        Response<List<Shipment>> response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find shipment failed,shopOrderId is ({})", shopOrderId);
            throw new JsonResponseException("find.shipment.failed");
        }
        //获取有效的销售发货单
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).
                filter(it -> Objects.equals(it.getType(), ShipmentType.SALES_SHIP.value())).collect(Collectors.toList());
        int count = 0;
        for (Shipment shipment : shipments) {
            ShipmentExtra shipmentExtra = this.getShipmentExtra(shipment);
            if (shipmentExtra.getShipmentShipFee() > 0) {
                count++;
            }
        }
        //如果已经有发货单计算过运费,返回true
        return count > 0;
    }

    private String getShareDiscount(SkuOrder skuOrder) {
        String skuShareDiscount = "";
        try {
            skuShareDiscount = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_SHARE_DISCOUNT);
        }
        return StringUtils.isEmpty(skuShareDiscount) ? "0" : skuShareDiscount;
    }

    public Long getPaymentIntegral(List<Long> skuOrderIds) {
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Long paymentIntegral = 0L;
        for (SkuOrder skuOrder : skuOrders) {
            paymentIntegral += getPaymentIntegral(skuOrder);
        }
        return paymentIntegral;
    }

    private Long getPaymentIntegral(SkuOrder skuOrder) {
        String paymentIntegral = "";
        try {
            paymentIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_PAYMENT_INTEGRAL, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_PAYMENT_INTEGRAL);
        }
        return StringUtils.isEmpty(paymentIntegral) ? 0L : Long.valueOf(paymentIntegral);
    }

    public Long getUsedIntegral(List<Long> skuOrderIds) {
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Long usedIntegral = 0L;
        for (SkuOrder skuOrder : skuOrders) {
            usedIntegral += getUsedIntegral(skuOrder);
        }
        return usedIntegral;
    }

    private Long getUsedIntegral(SkuOrder skuOrder) {
        String usedIntegral = "";
        try {
            usedIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_USED_INTEGRAL, skuOrder);
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_USED_INTEGRAL);
        }
        return StringUtils.isEmpty(usedIntegral) ? 0L : Long.valueOf(usedIntegral);
    }

    // 获取发货单涉及到的子单中已经发货的普通商品和积分商品所使用积分总数
    public Long getUsedIntegralPlusSkuIntegral(Map<Long, List<ShipmentItem>> skuOrderIdsAndShipmentItemInfosMapping) {
        List<Long> skuOrderIds = new ArrayList<>(skuOrderIdsAndShipmentItemInfosMapping.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Long sumOfSkuIntegralAndUsedIntegral = 0L;
        for (SkuOrder skuOrder : skuOrders) {
            sumOfSkuIntegralAndUsedIntegral += getUsedIntegralOrSkuIntegral(skuOrder,
                    skuOrderIdsAndShipmentItemInfosMapping.get(skuOrder.getId()));
        }
        return sumOfSkuIntegralAndUsedIntegral;
    }

    private Long getUsedIntegralOrSkuIntegral(SkuOrder skuOrder, List<ShipmentItem> shipmentItems) {
        String integral = "";
        Integer shipedItemQuantity = 0;
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipedItemQuantity += shipmentItem.getShipQuantity();
        }
        try {
            integral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_USED_INTEGRAL, skuOrder);
            if (StringUtils.isNotEmpty(integral) && !"0".equals(integral)
                    && shipedItemQuantity != skuOrder.getQuantity()) {
                BigDecimal integralCalculator = new BigDecimal(Long.valueOf(integral));
                integral = integralCalculator.divide(new BigDecimal(skuOrder.getQuantity()), 3, RoundingMode.HALF_DOWN)
                        .multiply(new BigDecimal(shipedItemQuantity)).setScale(0, RoundingMode.HALF_DOWN).toString();
            }
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(),
                    TradeConstants.SKU_USED_INTEGRAL);
        }
        //积分商品的积分不加如POS头
        /*try {
            if ("0".equals(integral) || StringUtils.isEmpty(integral)) {
                integral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL, skuOrder);
                if (StringUtils.isNotEmpty(integral) && !"0".equals(integral)) {
                    BigDecimal integralCalculator = new BigDecimal(Long.valueOf(integral));
                    integral = integralCalculator.multiply(BigDecimal.valueOf(shipedItemQuantity))
                            .setScale(0, RoundingMode.HALF_DOWN)
                            .toString();
                }

            }
        } catch (JsonResponseException e2) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_INTEGRAL);
        }*/
        return StringUtils.isEmpty(integral) ? 0L : Long.valueOf(integral);
    }

    // 获取发货单涉及到的子单中已经发货的普通商品和积分商品所使用积分抵扣金额总和
    public Long getAmountOfMoneyPaidByBothSkuIntegralAndUsedIntegral(
            Map<Long, List<ShipmentItem>> skuOrderIdsAndShipmentItemInfosMapping) {
        List<Long> skuOrderIds = new ArrayList<>(skuOrderIdsAndShipmentItemInfosMapping.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Long paymentIntegral = 0L;
        for (SkuOrder skuOrder : skuOrders) {
            paymentIntegral += getAmountOfMoneyPaidByBothSkuIntegralAndUsedIntegral(skuOrder,
                    skuOrderIdsAndShipmentItemInfosMapping.get(skuOrder.getId()));
        }
        return paymentIntegral;
    }

    private Long getAmountOfMoneyPaidByBothSkuIntegralAndUsedIntegral(SkuOrder skuOrder,
            List<ShipmentItem> shipmentItems) {
        String paymentIntegral = "";
        Integer shipedItemQuantity = 0;
        try {
            for (ShipmentItem shipmentItem : shipmentItems) {
                shipedItemQuantity += shipmentItem.getShipQuantity();
            }
            paymentIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_PAYMENT_INTEGRAL, skuOrder);
            if (StringUtils.isNotEmpty(paymentIntegral) && !"0".equals(paymentIntegral)
                    && shipedItemQuantity != skuOrder.getQuantity()) {
                BigDecimal paymentIntegralCalculator = new BigDecimal(Long.valueOf(paymentIntegral));
                paymentIntegral = paymentIntegralCalculator
                        .divide(new BigDecimal(skuOrder.getQuantity()), 3, RoundingMode.HALF_DOWN)
                        .multiply(new BigDecimal(shipedItemQuantity)).setScale(0, RoundingMode.HALF_DOWN).toString();
            }
        } catch (JsonResponseException e) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(),
                    TradeConstants.SKU_PAYMENT_INTEGRAL);
        }
        //积分商品的积分不加入POS头
       /* try {
            if ("0".equals(paymentIntegral) || StringUtils.isEmpty(paymentIntegral)) {
                String skuIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL, skuOrder);
                if (!StringUtils.isEmpty(skuIntegral) && !"0".equals(skuIntegral)) {
                  //默认积分，金额兑换比率为0.02，因此处返回金额需要乘以100，所以此处只做乘以2
                    paymentIntegral = String.valueOf(Math.multiplyExact(Math.multiplyExact(Long.valueOf(skuIntegral),2L)
                                    , shipedItemQuantity));
                }
            }
        } catch (JsonResponseException e2) {
            log.info("sku order(id:{}) extra map not contains key:{}", skuOrder.getId(), TradeConstants.SKU_INTEGRAL);
        }*/
        return StringUtils.isEmpty(paymentIntegral) ? 0L : Long.valueOf(paymentIntegral);
    }

    /**
     * 根据店铺订单主键查询发货单
     *
     * @param shopOrderId 店铺订单主键
     * @return
     */
    public List<Shipment> findByShopOrderId(Long shopOrderId) {
        Response<List<Shipment>> r = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!r.isSuccess()) {
            log.error("find shipment list by shop order id failed,shopOrderId is {},caused by {}", shopOrderId, r.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        return r.getResult();
    }

    /**
     * 根据店铺订单主键查询发货单
     *
     * @param shopOrderId 店铺订单主键
     * @return
     */
    public List<Shipment> findByShopOrderIdAndType(Long shopOrderId, Integer type) {
        Response<List<Shipment>> r = shipmentReadService.findByOrderIdAndOrderLevelAndType(shopOrderId, OrderLevel.SHOP, type);
        if (!r.isSuccess()) {
            log.error("find shipment list by shop order id failed,shopOrderId is {},caused by {}", shopOrderId, r.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        return r.getResult();
    }

    /**
     * 获取发货单集合下所有的发货单发货商品列表
     *
     * @param shipments
     * @return
     */
    public List<ShipmentItem> getShipmentItemsForList(List<Shipment> shipments) {
        List<ShipmentItem> newShipmentItems = Lists.newArrayList();
        shipments.forEach(shipment -> {
            List<ShipmentItem> shipmentItems = this.getShipmentItems(shipment);
            newShipmentItems.addAll(shipmentItems);
        });
        return newShipmentItems;
    }

    /**
     * 判断该订单下是否存在可以撤单的发货单
     *
     * @param shopOrder 店铺订单
     * @return true 可以撤单， false 不可以撤单
     */
    public boolean isShopOrderCanRevoke(ShopOrder shopOrder) {

        //jit店铺订单不允许撤销
        if (Objects.equals(shopOrder.getShopId(), Long.valueOf(shopId))) {
            return false;
        }

        List<OrderShipment> orderShipments = this.findByOrderIdAndType(shopOrder.getId());
        java.util.Optional<OrderShipment> orderShipmentOptional = orderShipments.stream().findAny();
        if (!orderShipmentOptional.isPresent()) {
            return false;
        }
        List<Integer> orderShipmentStatus = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> !Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
                .map(OrderShipment::getStatus).collect(Collectors.toList());
        List<Integer> canRevokeStatus = Lists.newArrayList(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue()
                , MiddleShipmentsStatus.ACCEPTED.getValue(), MiddleShipmentsStatus.WAIT_SHIP.getValue(),
                MiddleShipmentsStatus.SYNC_HK_ACCEPT_FAILED.getValue(), MiddleShipmentsStatus.SYNC_HK_FAIL.getValue());
        for (Integer shipmentStatus : orderShipmentStatus) {
            if (!canRevokeStatus.contains(shipmentStatus)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据发货单获取sku详情
     *
     * @param shipment 发货单
     * @return
     */
    public List<SkuCodeAndQuantity> findShipmentSkuDetail(Shipment shipment) {
        List<ShipmentItem> shipmentItems = this.getShipmentItems(shipment);
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        //skuCode及数量
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (ShipmentItem shipmentItem : shipmentItems) {
            current.add(shipmentItem.getSkuCode(), shipmentItem.getQuantity());
        }
        Multiset<String> shipCurrent = ConcurrentHashMultiset.create();
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipCurrent.add(shipmentItem.getSkuCode(), shipmentItem.getShipQuantity());
        }

        for (String skuCode : current.elementSet()) {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuCode);
            skuCodeAndQuantity.setQuantity(current.count(skuCode));
            skuCodeAndQuantity.setShipQuantity(shipCurrent.count(skuCode));
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        }

        return skuCodeAndQuantities;
    }


    /**
     * 根据发货单获取sku详情
     *
     * @param shipment 发货单
     * @return
     */
    public List<SkuCodeAndQuantity> findShipmentSkuDetailForReject(Shipment shipment) {
        List<ShipmentItem> shipmentItems = this.getShipmentItems(shipment);
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        shipmentItems.forEach(shipmentItem -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuOrderId(shipmentItem.getSkuOrderId());
            skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
            skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        return skuCodeAndQuantities;
    }


    /**
     * 组装数据，用来处理库存
     *
     * @param shipment 发货单
     * @return
     */
    public DispatchOrderItemInfo getDispatchOrderItem(Shipment shipment) {
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        DispatchOrderItemInfo dispatchOrderItemInfo = new DispatchOrderItemInfo();
        dispatchOrderItemInfo.setOrderId(shipmentDetail.getShopOrder().getId());
        List<SkuCodeAndQuantity> skuCodeAndQuantities = this.findShipmentSkuDetail(shipment);
        if (Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)) {
            //店发改成仓发
            Long shopId = shipmentExtra.getWarehouseId();
            Shop shop = shopCacher.findShopById(shopId);
            WarehouseDTO warehouse = warehouseCacher.findByShopInfo(Joiner.on("_").join(Lists.newArrayList(shop.getOuterId(), shop.getBusinessId())));
            if (null == warehouse) {
                log.error(" find warehouse by code {} is null ", shop.getOuterId() + "-_" + shop.getBusinessId());
                throw new JsonResponseException("find.warehouse.failed");
            }
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouse.getId());
            warehouseShipment.setWarehouseName(warehouse.getWarehouseName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));
        } else {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(shipmentExtra.getWarehouseId());
            warehouseShipment.setWarehouseName(shipmentExtra.getWarehouseName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));
        }
        dispatchOrderItemInfo.setOpenShopId(shipment.getShopId());
        dispatchOrderItemInfo.setShipmentId(shipment.getId());
        dispatchOrderItemInfo.setSubOrderIds(Lists.transform(skuCodeAndQuantities, input -> input.getSkuOrderId()));
        return dispatchOrderItemInfo;
    }

    /**
     * 判断订单是否有多个发货单,true 表示只有一个发货单，false
     *
     * @param shopOrder
     * @return
     */
    public boolean isOrderHasMoreShipments(ShopOrder shopOrder) {
        try {
            List<Shipment> shipments = findByShopOrderId(shopOrder.getId());
            List<Shipment> shipmentsFilter = shipments.stream().filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
            if (shipmentsFilter.size() > 1) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("find shipment faild,order id is {},cauesd by {}", shopOrder.getId(), Throwables.getStackTraceAsString(e));
            return true;
        }
    }

    /**
     * 根据售后单号获取有效的发货单商品
     *
     * @param afterSaleCode 售后单号
     * @return
     */
    public List<ShipmentItem> findAfterSaleShipmentItems(String afterSaleCode) {
        OrderShipmentCriteria orderShipmentCriteria = new OrderShipmentCriteria();
        orderShipmentCriteria.setAfterSaleOrderCode(afterSaleCode);
        Response<Paging<ShipmentPagingInfo>> pagingResponse = orderShipmentReadService.findBy(orderShipmentCriteria);
        if (!pagingResponse.isSuccess()) {
            throw new JsonResponseException("find.shipments.failed");
        }
        List<ShipmentPagingInfo> shipmentPagingInfos = pagingResponse.getResult().getData();
        List<ShipmentItem> shipmentItems = Lists.newArrayList();
        for (ShipmentPagingInfo shipmentPagingInfo : shipmentPagingInfos) {
            Shipment shipment = shipmentPagingInfo.getShipment();
            if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue())
                    || Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())) {
                continue;
            }
            ShipmentExtra shipmentExtra = this.getShipmentExtra(shipment);
            List<ShipmentItem> shipmentItemList = this.getShipmentItems(shipment);
            //用于添加warehouseid
            List<ShipmentItem> newShipmentItemList = Lists.newArrayList();
            //添加发货仓库
            for (ShipmentItem shipmentItem : shipmentItemList) {
                shipmentItem.setWarehouseId(shipmentExtra.getWarehouseId());
                //                shipmentItem.setItemWarehouseName(shipmentExtra.getWarehouseName());
                newShipmentItemList.add(shipmentItem);
            }
            shipmentItems.addAll(newShipmentItemList);
        }
        //根据货品条码去重
        Map<Object, ShipmentItem> skuCodeShipmentItems = Maps.newHashMap();
        for (ShipmentItem shipmentItem : shipmentItems) {
            skuCodeShipmentItems.put(SkuCodeUtil.getCombineCode(shipmentItem), shipmentItem);
        }
        return Lists.newArrayList(skuCodeShipmentItems.values());
    }

    /**
     * 根据订单号获取有效的发货单商品
     *
     * @param shopOrderCode 店铺订单号
     * @return
     */
    public List<ShipmentItem> findShopOrderShipmentItems(String shopOrderCode) {
        //获取订单下的所有有效发货单
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(shopOrderCode);
        List<Shipment> originShipments = this.findByShopOrderId(shopOrder.getId());
        List<Shipment> shipments = originShipments.stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))
                .collect(Collectors.toList());
        //获取订单下对应发货单的所有发货商品列表
        List<ShipmentItem> shipmentItems = this.getShipmentItemsForList(shipments);
        return shipmentItems;
    }

    public List<ShipmentItem> findByShipmentId(Long shipmentId) {
        Response<List<ShipmentItem>> r = shipmentItemReadService.findByShipmentId(shipmentId);
        if (!r.isSuccess()) {
            log.error("find shipment item list by shipment id failed,shipmentId is {},caused by {}", shipmentId, r.getError());
            throw new JsonResponseException("shipment.item.find.fail");
        }
        return r.getResult();
    }


    public List<ShipmentItem> findShipmentItems(ShipmentItemCriteria criteria) {
        Response<List<ShipmentItem>> r = psShipmentItemReadService.findShipmentItems(criteria);
        if (!r.isSuccess()) {
            log.error("find shipment item list by {},caused by {}", criteria.toString(), r.getError());
            throw new JsonResponseException("shipment.item.find.fail");
        }
        return r.getResult();
    }

    public List<Shipment> findByOrderIdAndSkuCode(Long shopOrderId, String skuCode, List<MiddleShipmentsStatus> exclude) {
        List<Shipment> shipments = findByShopOrderId(shopOrderId);
        if (CollectionUtils.isEmpty(shipments)) {
            return Collections.emptyList();
        }

        if (exclude == null) {
            exclude = Lists.newArrayList(MiddleShipmentsStatus.REJECTED, MiddleShipmentsStatus.CANCELED);
        }
        List<MiddleShipmentsStatus> exclusion = exclude;
        shipments = shipments.stream()
                .filter(Objects::nonNull)
                .filter(it -> !exclusion.contains(MiddleShipmentsStatus.fromInt(it.getStatus())))
                .collect(Collectors.toList());

        List<Shipment> availShipments = Lists.newArrayList();
        for (Shipment shipment : shipments) {
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            long count = shipmentItems.stream().
                    filter(Objects::nonNull)
                    .filter(shipmentItem -> Objects.equals(shipmentItem.getSkuCode(), skuCode))
                    .count();

            if (count > 0) {
                availShipments.add(shipment);
            }
        }
        return availShipments;
    }

    /**
     * 是否是补邮费订单
     *
     * @param shipmentExtra
     * @return
     */
    private boolean isPostageOrder(ShipmentExtra shipmentExtra) {
        return !Objects.isNull(shipmentExtra.getIsPostageOrder())
                && shipmentExtra.getIsPostageOrder();
    }
}
