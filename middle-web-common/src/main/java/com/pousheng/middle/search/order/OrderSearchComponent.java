package com.pousheng.middle.search.order;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.search.SearchedID;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-20 19:31<br/>
 */
@Slf4j
@Component
public class OrderSearchComponent {
    @Value("${order.search.indexName:orders}")
    private String indexName;
    @Value("${order.search.indexType:order}")
    private String indexType;
    @Value("${search.template:ps_search.mustache}")
    private String searchTemplate;
    /**
     * JIT店铺编号
     */
    @Value("${jit.open.shop.id}")
    private String shopId;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    private final Searcher searcher;
    private final OrderCriteriaBuilder orderCriteriaBuilder;
    private final MiddleOrderReadService middleOrderReadService;

    public OrderSearchComponent(Searcher searcher, OrderCriteriaBuilder orderCriteriaBuilder, MiddleOrderReadService middleOrderReadService) {
        this.searcher = searcher;
        this.orderCriteriaBuilder = orderCriteriaBuilder;
        this.middleOrderReadService = middleOrderReadService;
    }

    public Response<Paging<ShopOrderPagingInfo>> search(MiddleOrderCriteria criteria) {
        CriteriasWithShould c = orderCriteriaBuilder.build(criteria);
        Pagination<SearchedID> page = searcher.search(indexName, indexType, searchTemplate, c, SearchedID.class);
        return buildSearchResult(page);
    }


    private Response<Paging<ShopOrderPagingInfo>> buildSearchResult(Pagination<SearchedID> page) {
        List<Long> ids = page.getData().stream().map(SearchedID::getId).collect(Collectors.toList());
        Response<List<ShopOrder>> r = middleOrderReadService.findByOrderIds(ids);
        if (!r.isSuccess()) {
            throw new JsonResponseException(r.getError());
        }

        Map<Long, ShopOrderPagingInfo> idToOrder = Maps.newHashMap();
        Flow flow = MiddleFlowBook.orderFlow;
        List<ShopOrder> shopOrders = r.getResult();
        Paging<ShopOrderPagingInfo> pagingInfoPaging = Paging.empty();
        shopOrders.forEach(shopOrder -> {
            ShopOrderPagingInfo shopOrderPagingInfo = new ShopOrderPagingInfo();
            shopOrderPagingInfo.setShopOrder(shopOrder);
            //如果是mpos订单，不允许有其他操作。
            if (!shopOrder.getExtra().containsKey(TradeConstants.IS_ASSIGN_SHOP)) {
                String ecpOrderStatus = getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
                shopOrderPagingInfo.setShopOrderOperations(isShopOrderCanRevoke(shopOrder)
                        ? flow.availableOperations(shopOrder.getStatus())
                        : flow.availableOperations(shopOrder.getStatus()).stream().filter(it -> it.getValue() != MiddleOrderEvent.REVOKE.getValue()).collect(Collectors.toSet()));
            }
            //待处理的单子如果是派单失败的 允许出现 不包含有备注订单
            if (shopOrder.getOutFrom().equals(MiddleChannel.VIPOXO.getValue()) && shopOrder.getStatus().equals(MiddleOrderStatus.WAIT_HANDLE.getValue())) {
                if (shopOrder.getHandleStatus() != null && shopOrder.getHandleStatus() > OrderWaitHandleType.ORDER_HAS_NOTE.value()) {
                    Set<OrderOperation> orderOperations = Sets.newSet();
                    orderOperations.addAll(shopOrderPagingInfo.getShopOrderOperations());
                    orderOperations.add(MiddleOrderEvent.NOTICE_VIP_UNDERCARRIAGE.toOrderOperation());
                    shopOrderPagingInfo.setShopOrderOperations(orderOperations);
                }
            }
            idToOrder.put(shopOrder.getId(), shopOrderPagingInfo);
        });
        //撤销时必须保证订单没有发货
        List<ShopOrderPagingInfo> pagingInfos = ids.stream().map(idToOrder::get).filter(Objects::nonNull).collect(Collectors.toList());
        pagingInfoPaging.setData(pagingInfos);
        pagingInfoPaging.setTotal(page.getTotal());
        return Response.ok(pagingInfoPaging);
    }

    /**
     * 根据key获取交易单extraMap中的value
     *
     * @param key       key
     * @param shopOrder 交易单
     * @return value
     */

    private String getOrderExtraMapValueByKey(String key, ShopOrder shopOrder) {
        Map<String, String> extraMap = shopOrder.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("shop order(id:{}) extra map is empty", shopOrder.getId());
            throw new JsonResponseException("shop.order.extra.is.null");
        }
        if (!extraMap.containsKey(key)) {
            log.error("shop order(id:{}) extra map not contains key:{}", shopOrder.getId(), key);
            throw new JsonResponseException("shop.order.extra.not.contains.valid.key");
        }
        return extraMap.get(key);

    }

    /**
     * 判断该订单下是否存在可以撤单的发货单
     *
     * @param shopOrder 店铺订单
     * @return true 可以撤单， false 不可以撤单
     */
    private boolean isShopOrderCanRevoke(ShopOrder shopOrder) {

        //jit店铺订单不允许撤销
        if (Objects.equals(shopOrder.getShopId(), Long.valueOf(shopId))) {
            return false;
        }

        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()) {
            log.error("find order shipment by order id:{} level:{} fail,error:{}", shopOrder.getId(), OrderLevel.SHOP.toString(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<OrderShipment> orderShipments = response.getResult();
        Optional<OrderShipment> orderShipmentOptional = orderShipments.stream().findAny();
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
}
