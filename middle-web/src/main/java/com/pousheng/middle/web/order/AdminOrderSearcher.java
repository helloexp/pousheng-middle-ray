package com.pousheng.middle.web.order;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.search.SearchedID;
import com.pousheng.middle.search.order.OrderSearchComponent;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.swagger.annotations.Api;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.constants.JitConsts;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.mockito.internal.util.collections.Sets;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-20 19:27<br/>
 */
@Api(tags = "订单读服务搜索API")
@RestController
@Slf4j
@PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
@OperationLogModule(OperationLogModule.Module.ORDER)
public class AdminOrderSearcher {
    private final PermissionUtil permissionUtil;
    private final MiddleOrderFlowPicker flowPicker;
    private final OrderSearchComponent orderSearchComponent;
    private final MiddleOrderReadService middleOrderReadService;
    private final OrderReadLogic orderReadLogic;
    private final ShipmentReadLogic shipmentReadLogic;

    public AdminOrderSearcher(PermissionUtil permissionUtil, MiddleOrderFlowPicker flowPicker, OrderSearchComponent orderSearchComponent,
                              MiddleOrderReadService middleOrderReadService, OrderReadLogic orderReadLogic, ShipmentReadLogic shipmentReadLogic) {
        this.flowPicker = flowPicker;
        this.permissionUtil = permissionUtil;
        this.orderSearchComponent = orderSearchComponent;
        this.middleOrderReadService = middleOrderReadService;
        this.orderReadLogic = orderReadLogic;
        this.shipmentReadLogic = shipmentReadLogic;
    }

    @GetMapping("/api/order/search")
    public Response<Paging<ShopOrderPagingInfo>> search(MiddleOrderCriteria middleOrderCriteria) {
        // 店铺
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (middleOrderCriteria.getShopId() == null) {
            middleOrderCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(middleOrderCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        // 开始结束时间
        if (middleOrderCriteria.getOutCreatedStartAt() == null) {
            middleOrderCriteria.setOutCreatedStartAt(DateTime.now().withTimeAtStartOfDay().minusMonths(1).toDate());
        }
        if (middleOrderCriteria.getOutCreatedEndAt() == null) {
            middleOrderCriteria.setOutCreatedEndAt(DateTime.now().withTimeAtStartOfDay().plusDays(1).toDate());
        }
        long diff = middleOrderCriteria.getOutCreatedEndAt().getTime() - middleOrderCriteria.getOutCreatedStartAt().getTime();
        diff = diff / 1000 / 60 / 60 / 24;
        if (diff > 32) {
            return Response.fail("over 30 days");
        }
        // 不显示jit时效订单来源
        middleOrderCriteria.setExcludeOutFrom(JitConsts.YUNJU_REALTIME);

        // es 搜索
        Pagination<SearchedID> page = orderSearchComponent.search(middleOrderCriteria);
        if (page.getTotal() == 0 || page.getData().isEmpty()) {
            return Response.ok(Paging.empty());
        }

        return buildSearchResult(page);
    }

    private Response<Paging<ShopOrderPagingInfo>> buildSearchResult(Pagination<SearchedID> page) {
        List<Long> ids = page.getData().stream().map(SearchedID::getId).collect(Collectors.toList());
        Response<List<ShopOrder>> r = middleOrderReadService.findByOrderIds(ids);
        if (!r.isSuccess()) {
            throw new JsonResponseException(r.getError());
        }

        Map<Long, ShopOrderPagingInfo> idToOrder = Maps.newHashMap();
        Flow flow = flowPicker.pickOrder();
        List<ShopOrder> shopOrders = r.getResult();
        Paging<ShopOrderPagingInfo> pagingInfoPaging = Paging.empty();
        shopOrders.forEach(shopOrder -> {
            ShopOrderPagingInfo shopOrderPagingInfo = new ShopOrderPagingInfo();
            shopOrderPagingInfo.setShopOrder(shopOrder);
            //如果是mpos订单，不允许有其他操作。
            if (!shopOrder.getExtra().containsKey(TradeConstants.IS_ASSIGN_SHOP)) {
                String ecpOrderStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
                shopOrderPagingInfo.setShopOrderOperations(shipmentReadLogic.isShopOrderCanRevoke(shopOrder)
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
}
