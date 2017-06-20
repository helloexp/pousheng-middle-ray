package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.manager.WarehouseRuleItemManager;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleItemWriteServiceImpl implements WarehouseRuleItemWriteService {

    private final WarehouseRuleItemManager warehouseRuleItemManager;

    @Autowired
    public WarehouseRuleItemWriteServiceImpl(WarehouseRuleItemManager warehouseRuleItemManager) {
        this.warehouseRuleItemManager = warehouseRuleItemManager;
    }

    /**
     * 批量保存WarehouseRuleItems
     *
     * @param ruleId 规则id
     * @param warehouseRuleItems 列表
     * @return 是否创建成功
     */
    @Override
    public Response<Boolean> batchCreate(Long ruleId, List<WarehouseRuleItem> warehouseRuleItems) {
        try {
            int priority = 1;
            for (WarehouseRuleItem ruleItem : warehouseRuleItems) {
                ruleItem.setRuleId(ruleId);
                ruleItem.setPriority(priority);
                priority++;
            }
            warehouseRuleItemManager.batchCreate(ruleId, warehouseRuleItems);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to batch create wareRuleItems for ruleId({}), cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.batch.create.fail");
        }
    }
}
