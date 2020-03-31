package com.pousheng.middle.consume.index.cacher;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.inventory.api.service.PoushengWarehouseRuleReadService;
import com.pousheng.inventory.domain.model.PoushengWarehouseRule;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 17:53<br/>
 */
@Slf4j
@Component
public class WarehouseRuleCacher {
    @RpcConsumer(version = "1.0.0")
    private PoushengWarehouseRuleReadService poushengWarehouseRuleReadService;

    private LoadingCache<Long, Optional<PoushengWarehouseRule>> ruleCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadRuleById));

    private LoadingCache<Long, List<PoushengWarehouseRule>> rulesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadRulesById));

    private Optional<PoushengWarehouseRule> loadRuleById(Long ruleId) {
        try {
            Response<PoushengWarehouseRule> r = poushengWarehouseRuleReadService.findById(ruleId);
            if (!r.isSuccess()) {
                log.error("fail to find warehouse rule by id: {}, cause:{}", ruleId, r.getError());
                return Optional.empty();
            }
            return Optional.ofNullable(r.getResult());
        } catch (Exception e) {
            log.error("fail to find warehouse rule by id: {}, cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Optional.empty();
        }
    }

    private List<PoushengWarehouseRule> loadRulesById(Long groupId) {
        try {
            Response<List<PoushengWarehouseRule>> r = poushengWarehouseRuleReadService.findByGroupId(groupId);
            if (!r.isSuccess()) {
                log.error("failed to find rules by group id {}, cause{}", groupId, r.getError());
                return Collections.emptyList();
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("fail , cause:{}", Throwables.getStackTraceAsString(e));
            return Collections.emptyList();
        }
    }

    public PoushengWarehouseRule findById(Long ruleId) {
        return ruleCache.getUnchecked(ruleId).orElse(null);
    }

    public List<PoushengWarehouseRule> findByGroupId(Long groupId) {
        return rulesCache.getUnchecked(groupId);
    }
}
