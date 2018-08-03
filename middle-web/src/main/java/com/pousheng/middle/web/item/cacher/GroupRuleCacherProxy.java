package com.pousheng.middle.web.item.cacher;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
import com.pousheng.middle.group.service.ItemRuleShopReadService;
import com.pousheng.middle.group.service.ItemRuleWarehouseReadService;
import com.pousheng.middle.web.mq.group.CacherName;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/14
 */
@Slf4j
@Component
@CacheConfig(cacheNames = "GroupRule")
public class GroupRuleCacherProxy {

    @RpcConsumer
    private ItemRuleGroupReadService itemRuleGroupReadService;

    @RpcConsumer
    private ItemRuleShopReadService itemRuleShopReadService;

    @RpcConsumer
    private ItemRuleWarehouseReadService itemRuleWarehouseReadService;

    @Autowired
    private TerminusMQProducer producer;

    @Value("${terminus.rocketmq.cacherClearTopic}")
    private String cacherClearTopic;



    /**
     * 通过店铺获取所属分组
     */
    @Cacheable(key = "'group#findByShopId:'.concat(#shopId.hashCode())")
    public List<Long> findByShopId(Long shopId) {
        Response<Long> ruleResp = itemRuleShopReadService.findRuleIdByShopId(shopId);
        if (!ruleResp.isSuccess()) {
            log.error("failed to find rule (shopId={}, error:{}",
                    shopId, ruleResp.getError());
            throw new ServiceException("item.rule.find.fail");
        }
        if (ruleResp.getResult() == null) {
            return Lists.newArrayList();
        }
        Response<List<ItemRuleGroup>> groupResp = itemRuleGroupReadService.findByRuleId(ruleResp.getResult());
        if (!groupResp.isSuccess()) {
            log.error("failed to find rule group (ruleId={}, error:{}",
                    ruleResp.getResult(), ruleResp.getError());
            throw new ServiceException("item.group.find.fail");
        }
        List<ItemRuleGroup> groups = groupResp.getResult();
        return groups.stream().map(ItemRuleGroup::getGroupId).collect(Collectors.toList());
    }


    @Cacheable(key = "'group#findByWarehouseId:'.concat(#warehouseId.hashCode())")
    public List<Long> findByWarehouseId(Long warehouseId) {
        Response<Long> ruleResp = itemRuleWarehouseReadService.findRuleIdByWarehouseId(warehouseId);
        if (!ruleResp.isSuccess()) {
            log.error("failed to find rule (warehouseId={}, error:{}",
                    warehouseId, ruleResp.getError());
            throw new ServiceException("item.rule.find.fail");
        }
        if (ruleResp.getResult() == null) {
            return Lists.newArrayList();
        }
        Response<List<ItemRuleGroup>> groupResp = itemRuleGroupReadService.findByRuleId(ruleResp.getResult());
        if (!groupResp.isSuccess()) {
            log.error("failed to find rule group (ruleId={}, error:{}",
                    ruleResp.getResult(), ruleResp.getError());
            throw new ServiceException("item.group.find.fail");
        }
        List<ItemRuleGroup> groups = groupResp.getResult();
        return groups.stream().map(ItemRuleGroup::getGroupId).collect(Collectors.toList());
    }


    @CacheEvict(allEntries = true)
    public void refreshAll() {
        producer.send(cacherClearTopic, CacherName.GROUP_RULE.value());
    }


}
