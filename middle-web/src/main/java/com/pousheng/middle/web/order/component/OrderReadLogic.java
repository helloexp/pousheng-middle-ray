package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class OrderReadLogic {

    @RpcConsumer
    private OrderReadService orderReadService;

    @Autowired
    private ObjectMapper objectMapper;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private PaymentReadService paymentReadService;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private ExpressCodeReadService expressCodeReadService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    static final Integer BATCH_SIZE = 100;     // 批处理数量



    /**
     * 订单详情
     */
    public Response<OrderDetail> orderDetail(Long shopOrderId) {
        Response<OrderDetail> detailR = orderReadService.findOrderDetailById(shopOrderId);
        if (!detailR.isSuccess()) {
            // 这里直接返回交给herd处理
            return detailR;
        }

        return detailR;
        /*OrderDetail orderDetail = detailR.getResult();
        try {
            Flow flow = flowPicker.pick(orderDetail.getShopOrder(), OrderLevel.SHOP);
            orderDetail.setShopOrderOperations(pickCommonSkuOperation(orderDetail.getSkuOrders(), flow));
        } catch (Exception e) {
            log.error("fail to get shopOrder(id={}) detail's order operation, cause:{}, ignore",
                    shopOrderId, Throwables.getStackTraceAsString(e));
        }
        return Response.ok(orderDetail);*/
    }


    /**
     * 获取指定店铺订单下某些状态的子单
     * @param shopOrderId 店铺订单id
     * @param status 子单状态
     * @return 子单列表
     */
    public List<SkuOrder> findSkuOrderByShopOrderIdAndStatus(Long shopOrderId,Integer ...status){
        List<SkuOrder> skuOrders = Lists.newArrayList();
        OrderCriteria criteria = new OrderCriteria();
        criteria.setOrderId(shopOrderId);
        criteria.setStatus(Arrays.asList(status));

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,criteria ,skuOrders);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,criteria,skuOrders);
        }

        return skuOrders;
    }


    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,OrderCriteria criteria,List<SkuOrder> skuOrders) {

        Response<Paging<SkuOrder>> pagingRes = skuOrderReadService.findBy(pageNo, size, criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging sku order fail,criteria:{},error:{}",criteria,pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<SkuOrder> paging = pagingRes.getResult();
        List<SkuOrder> result = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(result)) {
            return Boolean.FALSE;
        }
        skuOrders.addAll(result);

        int current = result.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }

    /**
     * 根据店铺订单id查询子单
     * @param shopOrderId 店铺订单id
     * @return 子单集合
     */
    public List<SkuOrder> findSkuOrdersByShopOrderId(Long shopOrderId){
        Response<List<SkuOrder>> skuOrdersR = skuOrderReadService.findByShopOrderId(shopOrderId);
        if (!skuOrdersR.isSuccess()) {
            log.error("fail to find skuOrders by shopOrder id {}, error code:{}",
                    shopOrderId, skuOrdersR.getError());
            throw new JsonResponseException(skuOrdersR.getError());
        }

        return skuOrdersR.getResult();
    }

    public ShopOrder findShopOrderById(Long shopOrderId){
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(shopOrderId);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",shopOrderId,shopOrderRes.getError());
            throw new JsonResponseException(shopOrderRes.getError());
        }

        return shopOrderRes.getResult();
    }

    /**
     * 订单id集合查询子单
     * @param skuOrderIds 子订单id集合
     * @return 子单集合
     */
    public List<SkuOrder> findSkuOrdersByIds(List<Long> skuOrderIds){
        Response<List<SkuOrder>> skuOrdersR = skuOrderReadService.findByIds(skuOrderIds);
        if (!skuOrdersR.isSuccess()) {
            log.error("fail to find skuOrders by ids {}, error code:{}",
                    skuOrderIds, skuOrdersR.getError());
            throw new JsonResponseException(skuOrdersR.getError());
        }

        return skuOrdersR.getResult();
    }


    public List<Payment> findOrderPaymentInfo(Long orderId){

        Response<List<Payment>> paymentRes = paymentReadService.findByOrderIdAndOrderLevel(orderId,OrderLevel.SHOP);
        if(!paymentRes.isSuccess()){
            log.error("find order payment by order id:{} fail,error:{}",orderId,paymentRes.getError());
            throw new JsonResponseException(paymentRes.getError());
        }
        return paymentRes.getResult();

    }

    public List<Invoice> findInvoiceInfo(Long shopOrderId){

        Response<List<Invoice>> invoicesRes = middleOrderReadService.findInvoiceInfo(shopOrderId,OrderLevel.SHOP);
            if(!invoicesRes.isSuccess()){
            log.error("failed to find order invoice, order id={}, order level:{} cause:{}",shopOrderId, OrderLevel.SHOP.getValue(), invoicesRes.getError());
            throw new JsonResponseException(invoicesRes.getError());
        }
        return invoicesRes.getResult();
    }

    public ReceiverInfo findReceiverInfo(Long shopOrderId){
        Response<List<OrderReceiverInfo>> orderReceiverInfoRes = middleOrderReadService.findOrderReceiverInfo(shopOrderId,OrderLevel.SHOP);
        if(!orderReceiverInfoRes.isSuccess()){
            log.error("find order receiver info by order id:{} order level:{} fai,cause:{}",shopOrderId,OrderLevel.SHOP.getValue(),orderReceiverInfoRes.getError());
            throw  new JsonResponseException(orderReceiverInfoRes.getError());
        }

        return JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(orderReceiverInfoRes.getResult().get(0).getReceiverInfoJson(),ReceiverInfo.class);
    }


    /**
     * 根据key获取子单extraMap中的value
     * @param key key
     * @param skuOrder 子单
     * @return value
     */

    public String getSkuExtraMapValueByKey(String key,SkuOrder skuOrder){
        Map<String,String> extraMap = skuOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
           log.error("sku order(id:{}) extra map is empty",skuOrder.getId());
            throw new JsonResponseException("sku.order.extra.is.null");
        }
        if(!extraMap.containsKey(key)){
            log.error("sku order(id:{}) extra map not contains key:{}",skuOrder.getId(),key);
            throw new JsonResponseException("sku.order.extra.not.contains.valid.key");
        }
        return extraMap.get(key);

    }


    /**
     * 根据key获取交易单extraMap中的value
     * @param key key
     * @param shopOrder 交易单
     * @return value
     */

    public String getOrderExtraMapValueByKey(String key,ShopOrder shopOrder){
        Map<String,String> extraMap = shopOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("shop order(id:{}) extra map is empty",shopOrder.getId());
            throw new JsonResponseException("shop.order.extra.is.null");
        }
        if(!extraMap.containsKey(key)){
            log.error("shop order(id:{}) extra map not contains key:{}",shopOrder.getId(),key);
            throw new JsonResponseException("shop.order.extra.not.contains.valid.key");
        }
        return extraMap.get(key);

    }



    /**
     * 从sku订单总提取共有的操作作为店铺订单操作
     * @param skuOrders sku订单列表
     * @return 店铺订单操作列表
     */
    private Set<OrderOperation> pickCommonSkuOperation(Collection<SkuOrder> skuOrders, Flow flow) {
        //查询店铺操作,所有子订单共有的操作才能在订单级别操作
        ArrayListMultimap<OrderOperation, Long> groupSkuOrderIdByOperation = ArrayListMultimap.create();
        for (SkuOrder skuOrder : skuOrders) {
            Set<OrderOperation> orderOperations = flow.availableOperations(skuOrder.getStatus());
            for (OrderOperation orderOperation : orderOperations) {
                groupSkuOrderIdByOperation.put(orderOperation, skuOrder.getId());
            }
        }
        Set<OrderOperation> shopOperation = Sets.newHashSet();
        for (OrderOperation operation : groupSkuOrderIdByOperation.keySet()) {
            if (com.google.common.base.Objects.equal(groupSkuOrderIdByOperation.get(operation).size(), skuOrders.size())) {
                shopOperation.add(operation);
            }
        }
        return shopOperation;
    }

    public OrderBase findOrder(Long orderId, OrderLevel orderLevel) {
        switch (orderLevel) {
            case SHOP:
                Response<ShopOrder> shopOrderResp = shopOrderReadService.findById(orderId);
                if (!shopOrderResp.isSuccess()) {
                    log.error("fail to find shop order by id:{},cause:{}", orderId, shopOrderResp.getError());
                    throw new JsonResponseException(shopOrderResp.getError());
                }
                return shopOrderResp.getResult();
            case SKU:
                Response<SkuOrder> skuOrderResp = skuOrderReadService.findById(orderId);
                if (!skuOrderResp.isSuccess()) {
                    log.error("fail to find sku order by sku order id:{},cause:{}", orderId, skuOrderResp.getError());
                    throw new JsonResponseException(skuOrderResp.getError());
                }
                return skuOrderResp.getResult();
            default:
                throw new IllegalArgumentException("unknown.order.type");
        }
    }

    /**
     * 快递代码映射,根据店铺id获取
     * @param shopId 店铺主键
     * @return
     */
    public String getExpressCode(Long shopId,ExpressCode expressCode){
        Response<OpenShop> response = openShopReadService.findById(shopId);
        if (!response.isSuccess()){
            log.error("find openShop failed,shopId is {},caused by {}",shopId,response.getError());
            throw new JsonResponseException("find.openShop.failed");
        }
        OpenShop openShop = response.getResult();
        MiddleChannel channel =  MiddleChannel.from(openShop.getChannel());
        switch (channel){
            case JD:
                return expressCode.getJdCode();
            case TAOBAO:
                return expressCode.getTaobaoCode();
            case FENQILE:
                return expressCode.getFenqileCode();
            case SUNING:
                return expressCode.getSuningCode();
            case OFFICIAL:
                return expressCode.getPoushengCode();
            default:
                log.error("find express code failed");
                throw new JsonResponseException("find.expressCode.failed");
        }
    }

    /**
     * 根据恒康快递代码获取快递名称
     * @param hkExpressCode 恒康快递代码
     * @return  快递管理对象
     */
    public ExpressCode makeExpressNameByhkCode(String hkExpressCode) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setHkCode(hkExpressCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by hkCode:{}", hkExpressCode);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        return response.getResult().getData().get(0);
    }

    /**
     * 判断子单所属订单是否存在有效的发货单
     * @param skuId 子单id
     * @return true->没有生成过,false 生成过
     */
    public Boolean isShipmentCreated(Long skuId){
        SkuOrder skuOrder = (SkuOrder) this.findOrder(skuId,OrderLevel.SKU);
        long shopOrderId = skuOrder.getOrderId();
        Response<List<Shipment>>  response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find shipmemt by shopOrderId (={}) failed,",shopOrderId);
            throw new JsonResponseException("find.shipnent.failed");
        }
        List<Shipment> shipments = response.getResult().stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        if (shipments.size()>0){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    /**
     * 判断订单是否存在有效的发货单
     * @param shopOrderId 店铺订单主键
     * @return true->没有生成过,false 生成过
     */
    public Boolean isShipmentCreatedForShopOrder(Long shopOrderId){
        Response<List<Shipment>>  response = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find shipmemt by shopOrderId (={}) failed,",shopOrderId);
            throw new JsonResponseException("find.shipnent.failed");
        }
        List<Shipment> shipments = response.getResult().stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        if (shipments.size()>0){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public SkuOrder findSkuOrderByShopOrderIdAndSkuCode(long shopOrderId,String skuCode){
        List<SkuOrder> skuOrders = this.findSkuOrdersByShopOrderId(shopOrderId);
        List<SkuOrder> skuOrderFilters = skuOrders.stream().filter(Objects::nonNull).
                filter(skuOrder -> Objects.equals(skuCode,skuOrder.getSkuCode())).collect(Collectors.toList());
        if (skuOrderFilters.size()==0){
            throw new ServiceException("find.skuOrder.failed");
        }
        return skuOrderFilters.get(0);
    }
}
