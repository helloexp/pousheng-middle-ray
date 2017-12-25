package com.pousheng.middle.order.dispatch.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/12/25
 */
@Component
@Slf4j
public class DispatchComponent {


    /**
     * 获取查询roger返回的仓是否有整单发货的
     * @param hkSkuStockInfos 仓及商品集合
     * @param widskucode2stock 仓、商品、数量的table
     * @param skuCodeAndQuantities 商品编码和数量
     * @return 可以整单发货的仓
     */
    public List<WarehouseShipment> chooseSingleWarehouse(List<HkSkuStockInfo> hkSkuStockInfos, Table<Long, String, Integer> widskucode2stock,
                                                         List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<WarehouseShipment> singleWarehouses = Lists.newArrayListWithCapacity(hkSkuStockInfos.size());
        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfos) {
            List<WarehouseShipment> warehouseShipments = trySingleWarehouse(skuCodeAndQuantities, widskucode2stock, skuStockInfo);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                singleWarehouses.addAll(warehouseShipments);
            }
        }
        return singleWarehouses;
    }


    private List<WarehouseShipment> trySingleWarehouse(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Table<Long, String, Integer> widskucode2stock,
                                                       HkSkuStockInfo skuStockInfo) {


        Long warehouseId = skuStockInfo.getWarehouseId();
        List<HkSkuStockInfo.SkuAndQuantityInfo> materialList = skuStockInfo.getMaterial_list();
        Map<String, Integer> hkSkuQuantityMap = materialList.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(HkSkuStockInfo.SkuAndQuantityInfo::getBarcode, it -> it.getQuantity()));
        boolean enough = true;
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();

            if(!hkSkuQuantityMap.containsKey(skuCode)){
                enough = false;
                continue;
            }

            int stock = hkSkuQuantityMap.get(skuCode);
            widskucode2stock.put(warehouseId, skuCode, stock);
            if (stock < skuCodeAndQuantity.getQuantity()) {
                enough = false;
            }
        }
        if (enough) {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouseId);
            warehouseShipment.setWarehouseName(skuStockInfo.getWarehouseName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(warehouseShipment);
        }
        return Collections.emptyList();
    }
}
