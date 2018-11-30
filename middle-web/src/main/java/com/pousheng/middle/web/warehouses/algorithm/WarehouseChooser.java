package com.pousheng.middle.web.warehouses.algorithm;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseAddressRuleClient;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityReadService;
import com.pousheng.middle.web.shop.AdminShops;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 根据收货地址选择仓库的算法
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Component
@Slf4j
public class WarehouseChooser {

    @Autowired
    private WarehouseAddressRuleClient warehouseAddressRuleClient;
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @RpcConsumer
    private WarehouseRulePriorityReadService warehouseRulePriorityReadService;
    @RpcConsumer
    private WarehouseRulePriorityItemReadService warehouseRulePriorityItemReadService;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private AdminShops adminShops;

    private static final Ordering<WarehouseWithPriority> byPriority = Ordering.natural().onResultOf(new Function<WarehouseWithPriority, Integer>() {
        @Override
        public Integer apply(WarehouseWithPriority input) {
            return input.getPriority();
        }
    });


    /**
     * 根据收货地址id来查找可以发货的仓库
     *
     * @param addressId            地址id
     * @param skuCodeAndQuantities sku及数量
     * @return 对应的仓库及每个仓库应发货的数量
     */
    // TODO 修改是否合适
    public List<WarehouseShipment> choose(ShopOrder shopOrder, Long addressId, List<SkuCodeAndQuantity> skuCodeAndQuantities) {

        List<Long> addressIds = Lists.newArrayListWithExpectedSize(3);
        Long currentAddressId = addressId;
        addressIds.add(currentAddressId);
        while (currentAddressId > 1) {
            WarehouseAddress address = warehouseAddressCacher.findById(currentAddressId);
            addressIds.add(address.getPid());
            currentAddressId= address.getPid();
        }
        Boolean needSingle = false;
        //京东货到付款订单不允许拆分
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            needSingle = true;
        }
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
            needSingle = true;
        }
        Response<List<Warehouses4Address>> r = warehouseAddressRuleClient.findByReceiverAddressIds(shopOrder.getShopId(), addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopOrder.getShopId(), r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Warehouses4Address> warehouses4Addresses = r.getResult();
        for (Warehouses4Address warehouses4Address : warehouses4Addresses) {
            List<WarehouseWithPriority> warehouseWithPriorities = warehouses4Address.getWarehouses();
            List<WarehouseShipment> warehouseShipments = chooseWarehouse(shopOrder, byPriority.sortedCopy(warehouseWithPriorities),
                    skuCodeAndQuantities, needSingle);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                // 先锁定库存, 锁定成功后再返回结果
               /* Response<Boolean> rDecrease = mposSkuStockLogic.lockStock(genDispatchOrderInfo(shopOrder, skuCodeAndQuantities, warehouseShipments));
                if(!rDecrease.isSuccess()){
                    log.error("failed to decreaseStocks for addressId:{}, error code:{}," +
                            "auto dispatch stock failed", addressId, rDecrease.getError());
                    return Collections.emptyList();
                }*/

                return warehouseShipments;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 根据收货地址id来查找可以发货的仓库,通过优先级和距离进行排序
     *
     * @param receiverInfo         收货人信息
     * @param addressId            地址id
     * @param skuCodeAndQuantities sku及数量
     * @return 对应的仓库及每个仓库应发货的数量
     */
    public List<WarehouseShipment> chooseByRegion(ReceiverInfo receiverInfo, ShopOrder shopOrder, Long addressId, List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<Long> addressIds = Lists.newArrayListWithExpectedSize(3);
        Long currentAddressId = addressId;
        addressIds.add(currentAddressId);
        while (currentAddressId > 1) {
            WarehouseAddress address = warehouseAddressCacher.findById(currentAddressId);
            addressIds.add(address.getPid());
            currentAddressId= address.getPid();
        }
        Boolean needSingle = false;
        //京东货到付款订单不允许拆分
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            needSingle = true;
        }
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
            needSingle = true;
        }
        Response<List<Warehouses4Address>> r = warehouseAddressRuleClient.findByReceiverAddressIds(shopOrder.getShopId(), addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopOrder.getShopId(), r.getError());
            throw new JsonResponseException(r.getError());
        }

        //收货地址明细
        String address = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion() + receiverInfo.getDetail();
        String addressRegion = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion();

        List<Warehouses4Address> warehouses4Addresses = r.getResult();
        for (Warehouses4Address warehouses4Address : warehouses4Addresses) {
            List<WarehouseWithPriority> warehouseWithPriorities = initPriority(warehouses4Address,shopOrder);
            // 先按照优先级排序， 再对仓库到收货地址距离由近到远排序
            this.sortByPriorityAndRegion(warehouseWithPriorities, address, addressRegion);
            List<WarehouseShipment> warehouseShipments = chooseWarehouseForRegion(shopOrder, warehouseWithPriorities, skuCodeAndQuantities, needSingle);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                log.info("region warehouseShipments is {}", warehouseShipments);
                return warehouseShipments;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 对优先级仓库集合进行优先级和距离排序
     *
     * @param warehouseWithPrioritys
     * @param address
     * @param addressRegion
     */
    private void sortByPriorityAndRegion(List<WarehouseWithPriority> warehouseWithPrioritys, String address, String addressRegion) {
        Map<Long, DistanceDto> warehouseDistancesMap = warehouseAddressComponent.getWarehouseDistances(warehouseWithPrioritys, address, addressRegion);
        log.info("distance is {}, address is {} addressRegion is {}", warehouseDistancesMap.toString(), address, addressRegion);
        Comparator<WarehouseWithPriority> newLogicComparator = new Comparator<WarehouseWithPriority>() {
            @Override
            public int compare(WarehouseWithPriority w1, WarehouseWithPriority w2) {
                if (w1 == null) {
                    return 1;
                }

                if (w2 == null) {
                    return -1;
                }

                Integer p1 = w1.getPriority();
                Integer p2 = w2.getPriority();
                if (p1 == null && p2 != null) {
                    return 1;
                }
                if (p1 != null && p2 == null) {
                    return -1;
                }

                if (Objects.equals(p1, p2)) {
                    // 优先级相同按照距离进行排序
                    DistanceDto distanceDto1 = warehouseDistancesMap.get(w1.getWarehouseId());
                    DistanceDto distanceDto2 = warehouseDistancesMap.get(w2.getWarehouseId());
                    if (distanceDto1 == null) {
                        return 1;
                    }
                    if (distanceDto2 == null) {
                        return -1;
                    }

                    if (distanceDto1.getDistance() < distanceDto2.getDistance()) {
                        return -1;
                    } else if (distanceDto1.getDistance() > distanceDto2.getDistance()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } else {
                    // 按照优先级排序
                    return p1 - p2;

                }
            }
        };
        Collections.sort(warehouseWithPrioritys, newLogicComparator);
    }

    public DispatchOrderItemInfo genDispatchOrderInfo (ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantities, List<WarehouseShipment> warehouseShipments) {
        DispatchOrderItemInfo dispatchOrderItemInfo = new DispatchOrderItemInfo();
        dispatchOrderItemInfo.setOpenShopId(shopOrder.getShopId());
        dispatchOrderItemInfo.setOrderId(shopOrder.getId());
        dispatchOrderItemInfo.setSubOrderIds(Lists.transform(skuCodeAndQuantities, input -> input.getSkuOrderId()));
        dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
        dispatchOrderItemInfo.setSkuCodeAndQuantities(skuCodeAndQuantities);

        return dispatchOrderItemInfo;
    }


    private List<WarehouseShipment> chooseWarehouse(ShopOrder shopOrder, List<WarehouseWithPriority> warehouseWithPriorities,
                                                    List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                    Boolean needSingle) {
        log.info("sort result is {}", warehouseWithPriorities.toString());
        Table<Long, String, Integer> widskucode2stock = HashBasedTable.create();
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            Long warehouseId = warehouseWithPriority.getWarehouseId();
            List<WarehouseShipment> warehouseShipments = trySingleWarehouseByPriority(shopOrder, skuCodeAndQuantities, widskucode2stock, warehouseId);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                return warehouseShipments;
            }
        }
        //如果标记为只允许单一仓库发货
        if (needSingle) {
            return Collections.emptyList();
        }

        Map<String, Long> skuOrderCodeMap = Maps.newHashMap();


        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
            if (Arguments.notNull(skuCodeAndQuantity.getSkuOrderId())){
                skuOrderCodeMap.put(skuCodeAndQuantity.getSkuCode(),skuCodeAndQuantity.getSkuOrderId());
            }
        }

        List<WarehouseShipment> result = Lists.newArrayList();

        //总是选择可能发货数量最大的仓库
        while (current.size() > 0) {
            //当前可发货数量
            int affordCount = 0;
            Long candidateWarehouseId = -1L;

            for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
                Long warehouseId = warehouseWithPriority.getWarehouseId();
                if (!widskucode2stock.containsRow(warehouseId)){
                    continue;
                }

                //本仓库当前可以发货的数量
                int count = 0;
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(warehouseId, skuCode);
                    int actual = stock >= required ? required : 0;
                    count += actual;
                }
                if (count > affordCount) {
                    affordCount = count; //更新当前仓库的可发货数量
                    candidateWarehouseId = warehouseId;
                }


            }

            if (candidateWarehouseId < 0) {
                for (String skuCode : current.elementSet()) {
                    log.warn("insufficient sku(skuCode={}) stock: ", skuCode);
                }
                return Collections.emptyList();
            } else {//分配发货仓库
                WarehouseShipment warehouseShipment = new WarehouseShipment();
                warehouseShipment.setWarehouseId(candidateWarehouseId);
                warehouseShipment.setWarehouseName(warehouseCacher.findById(candidateWarehouseId).getWarehouseName());
                List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(candidateWarehouseId, skuCode);
                    int actual = stock >= required ? required : 0;

                    SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
                    scaq.setSkuOrderId(skuOrderCodeMap.get(skuCode));
                    scaq.setSkuCode(skuCode);
                    scaq.setQuantity(actual);
                    if (actual!=0){
                        scaqs.add(scaq);
                    }

                    //减少库存需求
                    current.remove(skuCode, actual);
                    //减少当前可用库存
                    widskucode2stock.put(candidateWarehouseId, skuCode, stock - actual);

                }
                warehouseShipment.setSkuCodeAndQuantities(scaqs);
                result.add(warehouseShipment);
            }
        }

        return result;
    }


    private List<WarehouseShipment> chooseWarehouseForRegion(ShopOrder shopOrder, List<WarehouseWithPriority> warehouseWithPriorities,
                                                             List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                             Boolean needSingle) {
        log.info("sort result is {}", warehouseWithPriorities.toString());
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);
        Map<Long, WarehouseWithPriority> map = warehouseWithPriorities.stream().collect(Collectors.toMap(WarehouseWithPriority::getWarehouseId, e -> e));
        List<Long> shopWarehouseIds = warehouseWithPriorities.stream().map(WarehouseWithPriority::getWarehouseId).collect(Collectors.toList());
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(shopWarehouseIds, skuCodes, shopOrder.getShopId());
        if (CollectionUtils.isEmpty(skuStockInfos)) {
            log.warn("not skuStockInfos so skip");
            return Collections.emptyList();
        }
        Table<Long, String, Integer> skuCodeQuantityTable = HashBasedTable.create();
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos) {
            // 检查店铺是否能接单  这里直接将不能接单的过滤掉 后面拆单就不需要再过滤 统一转换成仓发
            WarehouseDTO warehouseDTO = warehouseCacher.findByOutCodeAndBizId(hkSkuStockInfo.getStock_code(), hkSkuStockInfo.getCompany_id());
            if (Objects.equals(warehouseDTO.getWarehouseSubType(), WarehouseType.SHOP_WAREHOUSE.value())) {
                if (!adminShops.getShopCurrentStatus(hkSkuStockInfo.getStock_code(), Long.parseLong(hkSkuStockInfo.getCompany_id()))) {
                    warehouseWithPriorities.remove(map.get(warehouseDTO.getId()));
                    continue;
                } else {
                    hkSkuStockInfo.setBusinessId(warehouseDTO.getId());
                }
            }
            dispatchComponent.completeTotalWarehouseTab(hkSkuStockInfo, skuCodeQuantityTable);
        }
        log.info("skuCodeQuantityTable is {}", skuCodeQuantityTable);
        List<WarehouseShipment> warehouseShipments = dispatchComponent.chooseSingleWarehouse(skuCodeQuantityTable, skuCodeAndQuantities);
        if (!warehouseShipments.isEmpty()) {
            //table的循环是无顺序的  要重新排序
            Map<Long, WarehouseShipment> wMap = warehouseShipments.stream().collect(Collectors.toMap(WarehouseShipment::getWarehouseId, e -> e));
            for (Long id : shopWarehouseIds) {
                if (wMap.containsKey(id)) {
                    return Lists.newArrayList(wMap.get(id));
                }
            }
        }
        //如果标记为只允许单一仓库发货
        if (warehouseShipments.isEmpty() && needSingle) {
            return Collections.emptyList();
        }
        if (warehouseWithPriorities.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("after filter  sort result is {}", warehouseWithPriorities.toString());
        Map<String, Long> skuOrderCodeMap = Maps.newHashMap();
        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
            if (Arguments.notNull(skuCodeAndQuantity.getSkuOrderId())) {
                skuOrderCodeMap.put(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getSkuOrderId());
            }
        }

        List<WarehouseShipment> result = Lists.newArrayList();

        //总是选择可能发货数量最大的仓库
        while (current.size() > 0) {
            //当前可发货数量
            int affordCount = 0;
            Long candidateWarehouseId = -1L;

            for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
                Long warehouseId = warehouseWithPriority.getWarehouseId();
                if (!skuCodeQuantityTable.containsRow(warehouseId)) {
                    continue;
                }
                //本仓库当前可以发货的数量
                int count = 0;
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    log.info("warehouseId is {} .skuCode is {}", warehouseId, skuCode);
                    int stock = skuCodeQuantityTable.get(warehouseId, skuCode) == null ? 0 : skuCodeQuantityTable.get(warehouseId, skuCode);
                    int actual = stock >= required ? required : 0;
                    count += actual;
                }
                if (count > affordCount) {
                    affordCount = count; //更新当前仓库的可发货数量
                    candidateWarehouseId = warehouseId;
                }
            }

            if (candidateWarehouseId > 0) {
                WarehouseShipment warehouseShipment = new WarehouseShipment();
                warehouseShipment.setWarehouseId(candidateWarehouseId);
                warehouseShipment.setWarehouseName(warehouseCacher.findById(candidateWarehouseId).getWarehouseName());
                List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = skuCodeQuantityTable.get(candidateWarehouseId, skuCode) == null ? 0 : skuCodeQuantityTable.get(candidateWarehouseId, skuCode);
                    int actual = stock >= required ? required : 0;

                    SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
                    scaq.setSkuOrderId(skuOrderCodeMap.get(skuCode));
                    scaq.setSkuCode(skuCode);
                    scaq.setQuantity(actual);
                    if (actual != 0) {
                        scaqs.add(scaq);
                    }

                    //减少库存需求
                    current.remove(skuCode, actual);
                    //减少当前可用库存
                    skuCodeQuantityTable.put(candidateWarehouseId, skuCode, stock - actual);

                }
                warehouseShipment.setSkuCodeAndQuantities(scaqs);
                result.add(warehouseShipment);
            } else {
                for (String skuCode : current.elementSet()) {
                    log.warn("insufficient sku(skuCode={}) stock: ", skuCode);
                }
                return result;
            }
        }

        return result;
    }



    private List<WarehouseShipment> trySingleWarehouseByPriority(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Table<Long, String, Integer> widskucode2stock,
                                                                 Long warehouseId) {
        boolean enough = true;
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();
            List<HkSkuStockInfo> hkSkuStockInfos= queryHkWarhouseOrShopStockApi.doQueryStockInfo(Lists.newArrayList(warehouseId), Lists.newArrayList(skuCode), shopOrder.getShopId());
            int stock = 0;
            if (!ObjectUtils.isEmpty(hkSkuStockInfos) && !ObjectUtils.isEmpty(hkSkuStockInfos.get(0).getMaterial_list())) {
                stock = hkSkuStockInfos.get(0).getMaterial_list().get(0).getQuantity();
            }
            widskucode2stock.put(warehouseId, skuCode, stock);
            if (stock < skuCodeAndQuantity.getQuantity()) {
                enough = false;
            }
        }
        if (enough) {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouseId);
            WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
            warehouseShipment.setWarehouseName(warehouse.getWarehouseName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(warehouseShipment);
        }
        return Collections.emptyList();
    }

    /**
     * 初始化优先级排序
     * @param warehouses4Address
     * @param shopOrder
     * @return
     */
    private List<WarehouseWithPriority> initPriority(Warehouses4Address warehouses4Address, ShopOrder shopOrder) {
        Map<Long, Integer> priorityMap = new HashMap<>();
        RulePriorityCriteria criteria = new RulePriorityCriteria().ruleId(warehouses4Address.getWarehouseRule().getId()).searchDate(DateTime.now().withTimeAtStartOfDay().toDate()).status(1);
        Response<Paging<WarehouseRulePriority>> priorityResp = warehouseRulePriorityReadService.findByCriteria(criteria);
        List<WarehouseWithPriority> warehouseWithPriorities = Lists.newArrayList();
        warehouseWithPriorities.addAll(warehouses4Address.getTotalWarehouses());
        //过滤拒单过的门店仓
        List<Long> rejectShopIds = dispatchComponent.findRejectedShop(shopOrder.getId());
        if (!CollectionUtils.isEmpty(rejectShopIds)) {
            List<Long> rejectWarehouseIds = Lists.newArrayList();
            rejectShopIds.forEach(e -> {
                Shop shop = shopCacher.findShopById(e);
                rejectWarehouseIds.add(warehouseCacher.findByOutCodeAndBizId(shop.getOuterId(), shop.getBusinessId().toString()).getId());

            });
            log.info("reject shop warehouseIds is {}", rejectWarehouseIds);
            for (WarehouseWithPriority warehouseWithPriority : warehouses4Address.getShopWarehouses()) {
                if (!rejectWarehouseIds.contains(warehouseWithPriority.getWarehouseId())) {
                    warehouseWithPriorities.add(warehouseWithPriority);
                }
            }
        }else{
            warehouseWithPriorities.addAll(warehouses4Address.getShopWarehouses());
        }
        if (priorityResp.getResult().getTotal() > 0) {
            WarehouseRulePriority priority = priorityResp.getResult().getData().get(0);
            Response<List<WarehouseRulePriorityItem>> itemResp = warehouseRulePriorityItemReadService.findByPriorityId(priority.getId());
            if (!itemResp.isSuccess()) {
                throw new ServiceException(priorityResp.getError());
            }
            priorityMap = itemResp.getResult().stream().collect(Collectors.toMap(WarehouseRulePriorityItem::getWarehouseId, WarehouseRulePriorityItem::getPriority));

        }
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            warehouseWithPriority.setPriority(priorityMap.get(warehouseWithPriority.getWarehouseId()) == null ? Integer.MAX_VALUE : priorityMap.get(warehouseWithPriority.getWarehouseId()));
        }
        return warehouseWithPriorities;
    }


}
