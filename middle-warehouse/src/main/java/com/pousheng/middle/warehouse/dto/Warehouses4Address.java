package com.pousheng.middle.warehouse.dto;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 可以发送到指定地址的仓库列表
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Data
public class Warehouses4Address implements Serializable {

    /**
     * 规则id
     */
    private WarehouseRule warehouseRule;

    /**
     * 地址id
     */
    private Long addressId;

    /**
     * 全部仓库列表
     */
    private List<WarehouseWithPriority> warehouses;


    /**
     * 总仓列表
     */
    private List<WarehouseWithPriority> totalWarehouses;

    /**
     * 店仓列表
     */
    private List<WarehouseWithPriority> shopWarehouses;

    /**
     * 优先仓库列表
     */
    private Map<Integer, List<Long>> priorityWarehouseIds;

    /**
     * 优先店铺列表
     */
    private Map<Integer, List<Long>> priorityShopIds;


}
