package com.pousheng.middle.web.warehouses.algorithm;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @RpcConsumer
    private WarehouseAddressCacher warehouseAddressCacher;

    @RpcConsumer
    private WarehouseCacher warehouseCacher;

    @Autowired
    private StockPusher stockPusher;

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
    public List<WarehouseShipment> choose(Long shopId, Long addressId, List<SkuCodeAndQuantity> skuCodeAndQuantities) {

        List<Long> addressIds = Lists.newArrayListWithExpectedSize(3);
        Long currentAddressId = addressId;
        addressIds.add(currentAddressId);
        while (currentAddressId > 1) {
            WarehouseAddress address = warehouseAddressCacher.findById(currentAddressId);
            addressIds.add(address.getPid());
            currentAddressId= address.getPid();
        }

        Response<List<Warehouses4Address>> r = warehouseAddressRuleReadService.findByReceiverAddressIds(shopId, addressIds);
        if (!r.isSuccess()) {
            log.error("failed to find warehouses for addressIds:{} of shop(id={}), error code:{}",
                    addressIds, shopId, r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Warehouses4Address> warehouses4Addresses = r.getResult();
        for (Warehouses4Address warehouses4Address : warehouses4Addresses) {
            List<WarehouseWithPriority> warehouseWithPriorities = warehouses4Address.getWarehouses();
            List<WarehouseShipment> warehouseShipments = chooseWarehouse(byPriority.sortedCopy(warehouseWithPriorities),
                    skuCodeAndQuantities);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                return warehouseShipments;
            }
        }
        return Collections.emptyList();
    }



    private List<WarehouseShipment> chooseWarehouse(List<WarehouseWithPriority> warehouseWithPriorities,
                                                    List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        Table<Long, String, Integer> widskucode2stock = HashBasedTable.create();
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            Long warehouseId = warehouseWithPriority.getWarehouseId();
            List<WarehouseShipment> warehouseShipments = trySingleWarehouseByPriority(skuCodeAndQuantities, widskucode2stock, warehouseId);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                return warehouseShipments;
            }
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
            for (Long warehouseId : widskucode2stock.rowKeySet()) {
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
                warehouseShipment.setWarehouseName(warehouseCacher.findById(candidateWarehouseId).getName());
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

        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        for (WarehouseShipment warehouseShipment : result) {
            for (SkuCodeAndQuantity skuCodeAndQuantity : warehouseShipment.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }
        stockPusher.submit(skuCodes);
        return result;
    }


    private List<WarehouseShipment> trySingleWarehouseByPriority(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Table<Long, String, Integer> widskucode2stock,
                                                                 Long warehouseId) {
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
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouseId);
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            warehouseShipment.setWarehouseName(warehouse.getName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(warehouseShipment);
        }
        return Collections.emptyList();
    }
}
