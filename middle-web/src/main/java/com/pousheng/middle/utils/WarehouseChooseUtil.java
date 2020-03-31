package com.pousheng.middle.utils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import com.pousheng.middle.order.dispatch.dto.DispatchWithPriority;
import com.pousheng.middle.warehouse.dto.*;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author Xiongmin
 * 2019/3/19
 */
@Slf4j
public class WarehouseChooseUtil {

    /**
     * 选择最大可发货仓
     * @param warehouseWithPriorities
     * @param widskucode2stock
     * @param skuCodeAndQuantityMultimap
     * @return
     */
    public static Long getWarehouseIdForRegion(List<WarehouseWithPriority> warehouseWithPriorities,
                                               Table<Long, String, Integer> widskucode2stock,
                                               ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap) {
        int affordCount = 0;
        Long candidateWarehouseId = null;
        for (WarehouseWithPriority warehouseWithPriority : warehouseWithPriorities) {
            Long warehouseId = warehouseWithPriority.getWarehouseId();
            if (!widskucode2stock.containsRow(warehouseId)){
                continue;
            }
            //本仓库当前可以发货的数量
            int count = 0;
            for (String skuCode : skuCodeAndQuantityMultimap.keySet()) {
                List<SkuCodeAndQuantity> skuCodeAndQuantities = skuCodeAndQuantityMultimap.get(skuCode);
                int required = skuCodeAndQuantities.stream().mapToInt(SkuCodeAndQuantity::getQuantity).sum();
                int stock = MoreObjects.firstNonNull(widskucode2stock.get(warehouseId, skuCode), 0);
                int actual = stock >= required ? required : 0;
                count += actual;
            }
            if (count > affordCount) {
                affordCount = count;
                candidateWarehouseId = warehouseId;
            }
        }
        return candidateWarehouseId;
    }

    public static String getWarehouseIdForPackage(List<DispatchWithPriority> dispatchWithPriorities,
                                                Table<String, String, Integer> allSkuCodeQuantityTable,
                                               ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap) {
        log.info("warehouseChooseUtil.dispatchWithPriorities:{}", dispatchWithPriorities);
        log.info("warehouseChooseUtil.allSkuCodeQuantityTable:{}", allSkuCodeQuantityTable);
        log.info("warehouseChooseUtil.skuCodeAndQuantityMultimap:{}", skuCodeAndQuantityMultimap);
        int affordCount = 0;
        String candidateWarehouseId = null;
        for (DispatchWithPriority dispatchWithPriority : dispatchWithPriorities) {
            String warehouseOrShopId = dispatchWithPriority.getWarehouseOrShopId();
            //发货单下sku数量
            int count = 0;
            for (String skuCode : skuCodeAndQuantityMultimap.keySet()) {
                List<SkuCodeAndQuantity> skuCodeAndQuantities = skuCodeAndQuantityMultimap.get(skuCode);
                int required = skuCodeAndQuantities.stream().mapToInt(SkuCodeAndQuantity::getQuantity).sum();
                int stock = allSkuCodeQuantityTable.get(warehouseOrShopId, skuCode);
                int actual = stock >= required ? 1 : 0;
                count += actual;
            }
            if (count > affordCount) {
                affordCount = count;
                //距离最近拆单最少的
                candidateWarehouseId = warehouseOrShopId;
            }
        }
        log.info("warehouseChooseUtil.candidateWarehouseId:{}", candidateWarehouseId);
        return candidateWarehouseId;
    }

    public static void assignWarehouseShipment(ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap,
                                         List<WarehouseShipment> result,
                                         Table<Long, String, Integer> widskucode2stock,
                                         WarehouseDTO candidateWarehouseDTO) {
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        warehouseShipment.setWarehouseId(candidateWarehouseDTO.getId());
        warehouseShipment.setWarehouseName(candidateWarehouseDTO.getWarehouseName());
        List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
        List<String> matchedSkuCodes = Lists.newArrayList();
        for (String skuCode : skuCodeAndQuantityMultimap.keySet()) {
            List<SkuCodeAndQuantity> subSkuCodeAndQuantities = skuCodeAndQuantityMultimap.get(skuCode);
            int required = subSkuCodeAndQuantities.stream().mapToInt(SkuCodeAndQuantity::getQuantity).sum();
            int stock = MoreObjects.firstNonNull(widskucode2stock.get(candidateWarehouseDTO.getId(), skuCode), 0);
            int actual = stock >= required ? required : 0;
            if (actual > 0) {
                collectMatchedSkuCodeAndQuantity(scaqs, skuCode, subSkuCodeAndQuantities);
                matchedSkuCodes.add(skuCode);
                widskucode2stock.put(candidateWarehouseDTO.getId(), skuCode, stock - actual);
            }
        }
        for (String matchedSkuCode : matchedSkuCodes) {
            skuCodeAndQuantityMultimap.removeAll(matchedSkuCode);
        }
        warehouseShipment.setSkuCodeAndQuantities(scaqs);
        result.add(warehouseShipment);
    }

    public static void assignWarehouseShipment(ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap,
                                               List<WarehouseShipment> warehouseShipmentResult,
                                               List<ShopShipment> shopShipmentResult,
                                               Table<String, String, Integer> allSkuCodeQuantityTable,
                                               String candidateWarehouseOrShopId,
                                               String warehouseName,
                                               String shopName) {
        List<String> typeAndId = Splitters.COLON.splitToList(candidateWarehouseOrShopId);
        Long id = Long.valueOf(typeAndId.get(1));

        List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
        List<String> matchedSkuCodes = Lists.newArrayList();

        for (String skuCode : skuCodeAndQuantityMultimap.keySet()) {
            List<SkuCodeAndQuantity> subSkuCodeAndQuantities = skuCodeAndQuantityMultimap.get(skuCode);
            int required = subSkuCodeAndQuantities.stream().mapToInt(SkuCodeAndQuantity::getQuantity).sum();
            int stock = MoreObjects.firstNonNull(allSkuCodeQuantityTable.get(candidateWarehouseOrShopId, skuCode), 0);
            int actual = stock >= required ? required : 0;
            if (actual != 0) {
                collectMatchedSkuCodeAndQuantity(scaqs, skuCode, subSkuCodeAndQuantities);
                matchedSkuCodes.add(skuCode);
                allSkuCodeQuantityTable.put(candidateWarehouseOrShopId, skuCode, stock - actual);
            }
        }

        for (String matchedSkuCode : matchedSkuCodes) {
            skuCodeAndQuantityMultimap.removeAll(matchedSkuCode);
        }

        if (Objects.nonNull(warehouseName)) {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(id);
            warehouseShipment.setWarehouseName(warehouseName);
            warehouseShipment.setSkuCodeAndQuantities(scaqs);
            warehouseShipmentResult.add(warehouseShipment);

        }
        if (Objects.nonNull(shopName)) {
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(id);
            shopShipment.setShopName(shopName);
            shopShipment.setSkuCodeAndQuantities(scaqs);
            shopShipmentResult.add(shopShipment);
        }
    }

    public static ArrayListMultimap<String, SkuCodeAndQuantity> collectSkuCodeAndQuantityMultimap(
            List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap = ArrayListMultimap.create();
        for (SkuCodeAndQuantity it : skuCodeAndQuantities) {
            skuCodeAndQuantityMultimap.put(it.getSkuCode(), it);
        }
        return skuCodeAndQuantityMultimap;
    }

    private static void collectMatchedSkuCodeAndQuantity(List<SkuCodeAndQuantity> scaqs, String skuCode, List<SkuCodeAndQuantity> subSkuCodeAndQuantities) {
        for (SkuCodeAndQuantity skuCodeAndQuantity : subSkuCodeAndQuantities) {
            SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
            scaq.setSkuOrderId(skuCodeAndQuantity.getSkuOrderId());
            scaq.setSkuCode(skuCode);
            scaq.setQuantity(skuCodeAndQuantity.getQuantity());
            scaqs.add(scaq);
        }
    }
}
