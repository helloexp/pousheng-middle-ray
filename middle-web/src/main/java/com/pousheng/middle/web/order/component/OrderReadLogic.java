package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class OrderReadLogic {

    @Autowired
    private FlowPicker flowPicker;

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
}
