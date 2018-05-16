package com.pousheng.middle.web.shop.convert;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import io.terminus.common.exception.JsonResponseException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/2
 * Time: 下午1:53
 */
public class WarehouseRuleItemConverter {

    /**
     *
     * @param ruleId  规则id
     * @param warehouseList 仓库列表
     * @return  仓库发货规则项
     */
    public static List<WarehouseRuleItem> convertToWarehouseRuleItemList(Long ruleId, List<Warehouse> warehouseList) {
        List<WarehouseRuleItem> list = Lists.newArrayList();
        for (Warehouse warehouse : warehouseList) {
            WarehouseRuleItem warehouseRuleItem = new WarehouseRuleItem();
            warehouseRuleItem.setName(warehouse.getName());
            warehouseRuleItem.setRuleId(ruleId);
            warehouseRuleItem.setWarehouseId(warehouse.getId());
            list.add(warehouseRuleItem);
        }
        return list;
    }

    /**
     * 地址为空的仓库外码
     * @param warehouseList 仓库集合
     * @return 仓库外码
     */
    public static List<String> convertToWareHouseNullAddress(List<Warehouse> warehouseList) {
        List<String> list = Lists.newArrayList();
        for (Warehouse warehouse : warehouseList) {
            // 没有地址信息
            if (!StringUtils.hasText(warehouse.getAddress())) {
                Map<String, String> extra = warehouse.getExtra();
                if (extra.containsKey("outCode")) {
                    list.add(extra.get("outCode"));
                }
            }
        }
        return list;
    }

    /**
     * 非全渠道店铺发货仓
     * @param warehouseList 仓库集合
     * @return 仓库外码
     */
    public static List<String> convertToWareHouseNotContainShop(List<Warehouse> warehouseList) {
        List<String> list = Lists.newArrayList();
        for (Warehouse warehouse : warehouseList) {
            if (Objects.equal(WarehouseType.from(warehouse.getType()),WarehouseType.SHOP_WAREHOUSE)) {
                if (!StringUtils.hasText(warehouse.getAddress())) {
                    Map<String, String> extra = warehouse.getExtra();
                    if (extra.containsKey("outCode")) {
                        list.add(extra.get("outCode"));
                    }
                }
            }
        }
        return list;
    }


    /**
     *
     * @param exist     已经添加的
     * @param addList   准备添加的
     * @return          已经存在的仓库id
     */
    public static List<Long> convertToExistWareHouse(List<WarehouseRuleItem> exist, List<WarehouseRuleItem> addList) {
        List<Long> list = Lists.newArrayList();
        for (WarehouseRuleItem obj1 : exist) {
            for (WarehouseRuleItem obj2 : addList) {
                if (obj1.getWarehouseId() == obj2.getWarehouseId()) {
                    list.add(obj1.getWarehouseId());
                    continue;
                }
            }
        }
        return list;
    }

    /**
     * 获取仓库外码
     * @param warehouseList 仓库集合
     * @return 仓库外码
     */
    public static List<String> convertToWareHouseCodeList(List<Warehouse> warehouseList) {
        List<String> list = Lists.newArrayList();
        for (Warehouse warehouse : warehouseList) {
            Map<String, String> extra = warehouse.getExtra();
            if (extra.containsKey("outCode")) {
                list.add(extra.get("outCode"));
            }
        }
        return list;
    }
}
