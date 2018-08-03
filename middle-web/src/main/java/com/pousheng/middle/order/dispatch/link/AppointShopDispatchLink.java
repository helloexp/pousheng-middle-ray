package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.ReceiverInfoCompleter;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseAddressRuleClient;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指定门店发货规则
 * 优先级 1
 * 如果mpos订单指定了具体的门店发货，则直接整单派给该门店。
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class AppointShopDispatchLink implements DispatchOrderLink {

    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;
    @RpcConsumer
    private WarehouseAddressRuleClient warehouseAddressRuleClient;
    @Autowired
    private ReceiverInfoCompleter receiverInfoCompleter;
    @RpcConsumer
    private WarehouseRulePriorityReadService warehouseRulePriorityReadService;
    @RpcConsumer
    private WarehouseRulePriorityItemReadService warehouseRulePriorityItemReadService;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;


    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        log.info("DISPATCH-AppointShopDispatchLink-1  order(id:{}) start...", shopOrder.getId());
        Map<String, String> extraMap = shopOrder.getExtra();
        //1 代表指定门店发货
        if (extraMap.containsKey(TradeConstants.IS_ASSIGN_SHOP) && Objects.equal(extraMap.get(TradeConstants.IS_ASSIGN_SHOP), "1")) {
            Long shopId = Long.valueOf(extraMap.get(TradeConstants.ASSIGN_SHOP_ID));
            Shop shop = shopCacher.findShopById(shopId);
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(shopId);
            shopShipment.setShopName(shop.getName());
            shopShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            dispatchOrderItemInfo.getShopShipments().add(shopShipment);
            return Boolean.FALSE;
        }

        //查询当前店铺的派单仓范围

        List<Long> addressIds = Lists.newArrayListWithExpectedSize(3);
        if (Arguments.isNull(receiverInfo.getCityId())) {
            receiverInfoCompleter.complete(receiverInfo);
        }
        Long currentAddressId = Long.valueOf(receiverInfo.getCityId());
        addressIds.add(currentAddressId);
        while (currentAddressId > 1) {
            WarehouseAddress address = warehouseAddressCacher.findById(currentAddressId);
            addressIds.add(address.getPid());
            currentAddressId = address.getPid();
        }

        Response<List<Warehouses4Address>> r = warehouseAddressRuleClient.findByReceiverAddressIds(shopOrder.getShopId(), addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopOrder.getShopId(), r.getError());
            throw new ServiceException(r.getError());
        }
        if (CollectionUtils.isEmpty(r.getResult())) {
            log.error("no warehouse rule set for shop(id={})", shopOrder.getShopId());
            throw new ServiceException("warehouse.rule.not.found");
        }
        Warehouses4Address warehouses4Address = r.getResult().get(0);
        context.put(DispatchContants.WAREHOUSE_FOR_ADDRESS, fillPriorityInfo(warehouses4Address));
        return true;
    }


    private Warehouses4Address fillPriorityInfo(Warehouses4Address warehouses4Address) {
        RulePriorityCriteria criteria = new RulePriorityCriteria().ruleId(warehouses4Address.getWarehouseRule().getId()).searchDate(new Date()).status(1);
        Response<Paging<WarehouseRulePriority>> priorityResp = warehouseRulePriorityReadService.findByCriteria(criteria);
        List<Long> priorityWarehouseIds = Lists.newArrayList();
        List<Long> priorityShopIds = Lists.newArrayList();
        try {
            if (!priorityResp.isSuccess()) {
                throw new ServiceException(priorityResp.getError());
            }
            if (priorityResp.getResult().getTotal() > 0) {
                WarehouseRulePriority priority = priorityResp.getResult().getData().get(0);
                Response<List<WarehouseRulePriorityItem>> itemResp = warehouseRulePriorityItemReadService.findByPriorityId(priority.getId());
                if (!itemResp.isSuccess()) {
                    throw new ServiceException(priorityResp.getError());
                }
                List<WarehouseRulePriorityItem> items = itemResp.getResult();
                for (WarehouseRulePriorityItem item : items) {
                    WarehouseDTO dto = warehouseCacher.findById(item.getWarehouseId());
                    if (dto.getWarehouseSubType().equals(WarehouseType.TOTAL_WAREHOUSE.value())) {
                        priorityWarehouseIds.add(item.getWarehouseId());
                    } else {
                        priorityShopIds.add(middleShopCacher.findByOuterIdAndBusinessId(dto.getOutCode(), Long.parseLong(dto.getCompanyId())).getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("fail to fill priority item , skip it , cause by {}", e.getMessage());
        }
        warehouses4Address.setPriorityShopIds(priorityShopIds);
        warehouses4Address.setPriorityWarehouseIds(priorityWarehouseIds);
        //TODO 测试用 通过以后删除
        log.info("this rule {} priorityWarehouseIds is {} ", warehouses4Address.getWarehouseRule().getId(), priorityWarehouseIds.toString());
        log.info("this rule {} priorityShopIds is {} ", warehouses4Address.getWarehouseRule().getId(), priorityShopIds.toString());
        return warehouses4Address;
    }
}
