package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private UserReadService<User> userReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    /**
     * 分页查询订单列表
     */
    public Response<Paging<OrderGroup>> pagingOrder(Map<String, String> orderCriteria) {
        OrderCriteria criteria = objectMapper.convertValue(orderCriteria, OrderCriteria.class);

        if (criteria != null && !Strings.isNullOrEmpty(criteria.getMobile())) {
            Response<User> userR = userReadService.findBy(criteria.getMobile(), LoginType.MOBILE);
            if (!userR.isSuccess()) {
                log.error("fail to find user by mobile {}, error code:{}",
                        criteria.getMobile(), userR.getError());
                return Response.ok(Paging.empty(OrderGroup.class));
            } else {
                User user = userR.getResult();
                criteria.setBuyerId(user.getId());
            }
        }

        if (criteria != null && !Strings.isNullOrEmpty(criteria.getShopName())) {
            Response<Shop> shopR = shopReadService.findByName(criteria.getShopName());
            if (!shopR.isSuccess()) {
                log.error("fail to find shop by name {}, error code:{}",
                        criteria.getShopName(), shopR.getError());
                return Response.ok(Paging.empty(OrderGroup.class));
            } else {
                Shop shop = shopR.getResult();
                criteria.setShopId(shop.getId());
            }
        }

        Response<Paging<OrderGroup>> ordersR = orderReadService.findBy(criteria);
        if (!ordersR.isSuccess()) {
            //直接返回交给herd处理
            return ordersR;
        }
        Paging<OrderGroup> orderGroupPaging = ordersR.getResult();

        Multimap<Long, SkuOrder> byShopOrderId = ArrayListMultimap.create();
        groupSkuOrderByShopOrderId(byShopOrderId, orderGroupPaging.getData());

        for (OrderGroup orderGroup : orderGroupPaging.getData()) {
            try {
                //// TODO: 16/6/14 暂时只有在线支付一个流程
                Flow flow = flowPicker.pick(orderGroup.getShopOrder(), OrderLevel.SHOP);
                List<Long> skuIds = Lists.transform(orderGroup.getSkuOrderAndOperations(), new Function<OrderGroup.SkuOrderAndOperation, Long>() {
                    @Nullable
                    @Override
                    public Long apply(OrderGroup.SkuOrderAndOperation input) {
                        return input.getSkuOrder().getSkuId();
                    }
                });

                Response<List<Sku>> skuRes = skuReadService.findSkusByIds(skuIds);
                if(!skuRes.isSuccess()){
                    log.error("fail to find sku  by ids {} for order paging error:{}",
                            skuIds, skuRes.getError());
                    continue;
                }

                ImmutableMap<Long,Sku> skuIdAndSkuMap = Maps.uniqueIndex(skuRes.getResult(), new Function<Sku, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable Sku input) {
                        if(Arguments.isNull(input)){
                            return 0L;
                        }
                        return input.getId();
                    }
                });

                for (OrderGroup.SkuOrderAndOperation skuOrderAndOperation : orderGroup.getSkuOrderAndOperations()) {
                    SkuOrder skuOrder = skuOrderAndOperation.getSkuOrder();
                    skuOrderAndOperation.setSku(skuIdAndSkuMap.get(skuOrder.getSkuId()));
                    skuOrderAndOperation.setSkuOrderOperations(flow.availableOperations(skuOrder.getStatus()));
                }
                //确定店铺订单可以执行的操作
                //如果是根据状态筛选,那归组出来的子订单可能不能构成一个总单,这个时候就要以数据库真实数据为准
                //如果不根据状态筛选, 由于订单列表查询的时候只会返回有限数量的子订单,所以也要重新找一把
                orderGroup.setShopOrderOperations(
                        pickCommonSkuOperation(byShopOrderId.get(orderGroup.getShopOrder().getId()), flow));

            } catch (Exception e) {
                log.error("fail to find order operations by orderGroup {}, cause:{}, skip it",
                        orderGroup, Throwables.getStackTraceAsString(e));
            }
        }
        return Response.ok(orderGroupPaging);
    }

    private void groupSkuOrderByShopOrderId(Multimap<Long, SkuOrder> byShopOrderId, List<OrderGroup> orderGroups) {
        List<Long> shopOrderIds = Lists.transform(orderGroups, new Function<OrderGroup, Long>() {
            @Override
            public Long apply(OrderGroup input) {
                return input.getShopOrder().getId();
            }
        });
        Response<List<SkuOrder>> skuOrdersR = skuOrderReadService.findByShopOrderIds(shopOrderIds);
        if (!skuOrdersR.isSuccess()) {
            log.error("fail to find skuOrder by shopOrderIds {}, error code:{}",
                    shopOrderIds, skuOrdersR.getError());
            throw new JsonResponseException(skuOrdersR.getError());
        }
        List<SkuOrder> skuOrders = skuOrdersR.getResult();
        for (SkuOrder skuOrder : skuOrders) {
            byShopOrderId.put(skuOrder.getOrderId(), skuOrder);
        }
    }

    /**
     * 订单详情
     */
    public Response<OrderDetail> orderDetail(Long shopOrderId) {
        Response<OrderDetail> detailR = orderReadService.findOrderDetailById(shopOrderId);
        if (!detailR.isSuccess()) {
            // 这里直接返回交给herd处理
            return detailR;
        }
        OrderDetail orderDetail = detailR.getResult();
        try {
            //// TODO: 16/6/14 暂时只有在线支付一个流程
            Flow flow = flowPicker.pick(orderDetail.getShopOrder(), OrderLevel.SHOP);
            orderDetail.setShopOrderOperations(pickCommonSkuOperation(orderDetail.getSkuOrders(), flow));
        } catch (Exception e) {
            log.error("fail to get shopOrder(id={}) detail's order operation, cause:{}, ignore",
                    shopOrderId, Throwables.getStackTraceAsString(e));
        }
        return Response.ok(orderDetail);
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
