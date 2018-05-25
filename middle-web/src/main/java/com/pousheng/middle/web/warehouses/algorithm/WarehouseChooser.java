package com.pousheng.middle.web.warehouses.algorithm;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseAddressRuleClient;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        Response<List<Warehouses4Address>> r = warehouseAddressRuleClient.findByReceiverAddressIds(shopOrder.getShopId(), addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopOrder.getShopId(), r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Warehouses4Address> warehouses4Addresses = r.getResult();
        for (Warehouses4Address warehouses4Address : warehouses4Addresses) {
            List<WarehouseWithPriority> warehouseWithPriorities = warehouses4Address.getWarehouses();
            List<WarehouseShipment> warehouseShipments = chooseWarehouse(byPriority.sortedCopy(warehouseWithPriorities),
                    skuCodeAndQuantities, needSingle);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                // 先锁定库存, 锁定成功后再返回结果
                Response<Boolean> rDecrease = mposSkuStockLogic.lockStock(genDispatchOrderInfo(shopOrder, skuCodeAndQuantities, warehouseShipments));
                if(!rDecrease.isSuccess()){
                    log.error("failed to decreaseStocks for addressId:{}, error code:{}," +
                            "auto dispatch stock failed", addressId, rDecrease.getError());
                    return Collections.emptyList();
                }

                return warehouseShipments;
            }
        }
        return Collections.emptyList();
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


    private List<WarehouseShipment> chooseWarehouse(List<WarehouseWithPriority> warehouseWithPriorities,
                                                    List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                    Boolean needSingle) {
        Table<Long, String, Integer> widskucode2stock = HashBasedTable.create();
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            Long warehouseId = warehouseWithPriority.getWarehouseId();
            List<WarehouseShipment> warehouseShipments = trySingleWarehouseByPriority(skuCodeAndQuantities, widskucode2stock, warehouseId);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                return warehouseShipments;
            }
        }
        //如果标记为只允许单一仓库发货
        if (needSingle) {
            return Collections.emptyList();
        }
        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
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


    private List<WarehouseShipment> trySingleWarehouseByPriority(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Table<Long, String, Integer> widskucode2stock,
                                                                 Long warehouseId) {
        boolean enough = true;
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();
            Response<InventoryDTO> rStock = inventoryClient.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if (!rStock.isSuccess()) {
                log.error("failed to find sku(skuCode={}) in warehouse(id={}), error code:{}",
                        skuCode, warehouseId, rStock.getError());
                throw new ServiceException(rStock.getError());
            }
            int stock = rStock.getResult().getAvailStock().intValue();
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
}
