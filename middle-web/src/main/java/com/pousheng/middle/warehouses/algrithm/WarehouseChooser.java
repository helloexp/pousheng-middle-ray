package com.pousheng.middle.warehouses.algrithm;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.warehouses.dto.SelectedWarehouse;
import com.pousheng.middle.warehouses.dto.SkuCodeAndQuantity;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 根据收货地址选择仓库的算法
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Component
@Slf4j
public class WarehouseChooser {

    @RpcConsumer
    private WarehouseAddressRuleReadService warehouseAddressRuleReadService;

    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;

    private static final Ordering<WarehouseWithPriority> byPriority = Ordering.natural().onResultOf(new Function<WarehouseWithPriority, Integer>() {
        @Override
        public Integer apply(WarehouseWithPriority input) {
            return input.getPriority();
        }
    });


    public List<SelectedWarehouse> choose(List<Long> addressIds, List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        Response<List<Warehouses4Address>> r = warehouseAddressRuleReadService.findByReceiverAddressIds(addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{}, error code:{}", addressIds, r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Warehouses4Address> warehouses4Addresses = r.getResult();
        for (Warehouses4Address warehouses4Address : warehouses4Addresses) {
            List<WarehouseWithPriority> warehouseWithPriorities = warehouses4Address.getWarehouses();
            List<SelectedWarehouse> warehouses = chooseWarehouse(byPriority.sortedCopy(warehouseWithPriorities),
                    skuCodeAndQuantities);
            if (!CollectionUtils.isEmpty(warehouses)) {
                return warehouses;
            }
        }
        return Collections.emptyList();
    }

    private List<SelectedWarehouse> chooseWarehouse(List<WarehouseWithPriority> warehouseWithPriorities,
                                                    List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        Table<Long, String, Integer> widskucode2stock = HashBasedTable.create();
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            Long warehouseId = warehouseWithPriority.getWarehouseId();
            boolean enough = true;
            for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
                String skuCode = skuCodeAndQuantity.getSkuCode();
                Response<WarehouseSkuStock> rStock = warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
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
                SelectedWarehouse selectedWarehouse = new SelectedWarehouse();
                selectedWarehouse.setWarehouseId(warehouseId);
                selectedWarehouse.setWarehouseName(null);
                selectedWarehouse.setSkuCodeAndQuantities(skuCodeAndQuantities);
                return Lists.newArrayList(selectedWarehouse);
            }
        }
        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = HashMultiset.create(skuCodeAndQuantities.size());
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }

        List<SelectedWarehouse> result = Lists.newArrayList();

        //总是选择可能发货数量最大的仓库
        while (current.size() > 0) {
            int affordCount = 0;
            Long candidateWarehouseId = -1L;
            for (Long warehouseId : widskucode2stock.rowKeySet()) {
                //本仓库当前可以发货的数量
                int count = 0;
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(warehouseId, skuCode);
                    int actual = stock > required ? required : stock;
                    count += actual;
                }
                if (count > affordCount) {
                    candidateWarehouseId = warehouseId;
                }
            }
            if (candidateWarehouseId < 0) {
                for (String skuCode : current.elementSet()) {
                    log.warn("insufficient sku(skuCode={}) stock: ", skuCode);
                }
                return Collections.emptyList();
            } else {//分配发货仓库
                SelectedWarehouse selectedWarehouse = new SelectedWarehouse();
                selectedWarehouse.setWarehouseId(candidateWarehouseId);
                selectedWarehouse.setWarehouseName(null);
                List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(candidateWarehouseId, skuCode);
                    int actual = stock > required ? required : stock;
                    SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
                    scaq.setSkuCode(skuCode);
                    scaq.setQuantity(actual);
                    scaqs.add(scaq);

                    //减少库存需求
                    current.remove(skuCode, actual);
                    //减少当前可用库存
                    widskucode2stock.put(candidateWarehouseId, skuCode, stock - actual);
                }
                selectedWarehouse.setSkuCodeAndQuantities(scaqs);
                result.add(selectedWarehouse);
            }
        }
        return result;
    }
}
