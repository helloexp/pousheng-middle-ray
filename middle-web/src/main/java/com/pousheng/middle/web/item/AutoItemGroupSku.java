package com.pousheng.middle.web.item;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.item.enums.AttributeEnum;
import com.pousheng.middle.item.enums.AttributeRelationEnum;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.utils.task.ScheduleTaskUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */

@Slf4j
@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "false", matchIfMissing = false)
@RestController
public class AutoItemGroupSku {

    @Getter
    @Setter
    @RpcConsumer
    private ItemGroupReadService itemGroupReadService;

    @Autowired
    private HostLeader hostLeader;

    @RpcConsumer
    private ScheduleTaskWriteService scheduleTaskWriteService;

    @Autowired
    private BrandReadService brandReadService;


    /**
     * 每天凌晨1点触发
     */
    @RequestMapping("/api/item/group/handle")
    @Scheduled(cron = "0 0 1 * * ?")
    public void synchronizeSpu() {
        if (hostLeader.isLeader()) {
            log.info("JOB -- begin to auto item group sku");
            Response<List<ItemGroup>> resp = itemGroupReadService.findAutoGroups();
            if (!resp.isSuccess()) {
                throw new JsonResponseException(resp.getError());
            }
            List<ItemGroup> list = resp.getResult();
            for (ItemGroup group : list) {
                if (CollectionUtils.isEmpty(group.getGroupRule())) {
                    continue;
                }
                Map<String, String> params = constructParams(group.getGroupRule());
                try {
                    ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(params).groupId(group.getId())
                            .type(PsItemGroupSkuType.GROUP.value()).mark(true).userId(0L));
                    scheduleTaskWriteService.create(task);
                } catch (JsonResponseException e) {
                    log.info("auto item group fail, group id:{} cause by {} ", group.getId(), e.getMessage());
                }

                Map<String, String> removeParams = constructRemoveParams(group.getGroupRule());
                try {
                    ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(removeParams).groupId(group.getId())
                            .type(PsItemGroupSkuType.GROUP.value()).mark(false).userId(0L));
                    scheduleTaskWriteService.create(task);
                } catch (JsonResponseException e) {
                    log.info("auto item group fail, group id:{} cause by {} ", group.getId(), e.getMessage());
                }
            }
            log.info("JOB -- finish to auto item group sku");
        } else {
            log.info("host is not leader, so skip job");
        }
    }


    /**
     * 手工触发 指定分组
     * @param groupId
     */
    @RequestMapping("/api/item/group/{groupId}/handle")
    public void synchronizeSpu(@PathVariable Long groupId) {
        if (hostLeader.isLeader()) {
            log.info("JOB -- begin to auto item group sku");
            Response<ItemGroup> resp = itemGroupReadService.findById(groupId);
            if (!resp.isSuccess()) {
                throw new JsonResponseException(resp.getError());
            }
            ItemGroup group = resp.getResult();
            if (group.getAuto() && !CollectionUtils.isEmpty(group.getGroupRule())) {
                Map<String, String> params = constructParams(group.getGroupRule());
                try {
                    ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(params).groupId(group.getId())
                            .type(PsItemGroupSkuType.GROUP.value()).mark(true).userId(0L));
                    scheduleTaskWriteService.create(task);
                } catch (JsonResponseException e) {
                    log.info("auto item group fail, group id:{} cause by {} ", group.getId(), e.getMessage());
                }

                Map<String, String> removeParams = constructRemoveParams(group.getGroupRule());
                try {
                    ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(removeParams).groupId(group.getId())
                            .type(PsItemGroupSkuType.GROUP.value()).mark(false).userId(0L));
                    scheduleTaskWriteService.create(task);
                } catch (JsonResponseException e) {
                    log.info("auto item group fail, group id:{} cause by {} ", group.getId(), e.getMessage());
                }
            }
            log.info("JOB -- finish to auto item group sku");
        } else {
            log.info("host is not leader, so skip job");
        }
    }

    private Map<String, String> constructParams(List<ItemGroupAutoRule> rules) {

        Map<String, String> params = new HashMap<>(16);
        List<String> attrs = Lists.newArrayList(AttributeEnum.YEAR.value(),
                AttributeEnum.SEASON.value(),
                AttributeEnum.SEX.value(),
                AttributeEnum.CATEGORY.value(),
                AttributeEnum.SERIES.value(),
                AttributeEnum.STYLE.value());
        List<String> attrsVal = Lists.newArrayList();
        List<String> mustNotAttrsVal = Lists.newArrayList();
        for (ItemGroupAutoRule rule : rules) {
            if (StringUtils.isEmpty(rule.getValue())) {
                continue;
            }
            if (attrs.contains(rule.getName())) {
                List<String> ruleDetails = Splitters.COMMA.splitToList(rule.getValue());
                ruleDetails = ruleDetails.stream().map(e ->
                        e = AttributeEnum.from(rule.getName()) + ":" + e).collect(Collectors.toList());
                if (AttributeRelationEnum.IN.value().equals(rule.getRelation())) {
                    attrsVal.add(Joiners.COMMA.join(ruleDetails));
                } else {
                    mustNotAttrsVal.add(Joiners.COMMA.join(ruleDetails));
                }
            }
            if (rule.getName().equals(AttributeEnum.DAYS.value())) {
                List<String> dates = Splitter.on("-").splitToList(rule.getValue());
                if (dates.size() > 1) {
                    params.put("after", dates.get(0));
                    params.put("before", dates.get(1));
                } else {
                    if (AttributeRelationEnum.AFTER.value().equals(rule.getRelation())) {
                        params.put("after", dates.get(0));
                    }
                    if (AttributeRelationEnum.BEFORE.value().equals(rule.getRelation())) {
                        params.put("before", dates.get(0));
                    }
                }
            }
            if (rule.getName().equals(AttributeEnum.BRAND.value())) {
                String bids = contractBids(rule.getValue());
                if (AttributeRelationEnum.IN.value().equals(rule.getRelation())) {
                    params.put("bids", bids);
                } else {
                    params.put("mustNot_bids", bids);
                }
            }
        }
        if (mustNotAttrsVal.size() > 0) {
            params.put("mustNot_attrs", Joiner.on("_").join(mustNotAttrsVal));
        }
        if (attrsVal.size() > 0) {
            params.put("attrs", Joiner.on("_").join(attrsVal));
        }
        return params;
    }


    private Map<String, String> constructRemoveParams(List<ItemGroupAutoRule> rules) {

        Map<String, String> params = new HashMap<>(16);
        List<String> attrs = Lists.newArrayList(AttributeEnum.YEAR.value(),
                AttributeEnum.SEASON.value(),
                AttributeEnum.SEX.value(),
                AttributeEnum.CATEGORY.value(),
                AttributeEnum.SERIES.value(),
                AttributeEnum.STYLE.value());
        List<String> attrsVal = Lists.newArrayList();
        List<String> mustNotAttrsVal = Lists.newArrayList();
        for (ItemGroupAutoRule rule : rules) {
            if (StringUtils.isEmpty(rule.getValue())) {
                continue;
            }
            if (attrs.contains(rule.getName())) {
                List<String> ruleDetails = Splitters.COMMA.splitToList(rule.getValue());
                ruleDetails = ruleDetails.stream().map(e ->
                        e = AttributeEnum.from(rule.getName()) + ":" + e).collect(Collectors.toList());
                if (AttributeRelationEnum.IN.value().equals(rule.getRelation())) {
                    attrsVal.add(Joiners.COMMA.join(ruleDetails));
                } else {
                    mustNotAttrsVal.add(Joiners.COMMA.join(ruleDetails));
                }
            }
            if (rule.getName().equals(AttributeEnum.DAYS.value())) {
                List<String> dates = Splitter.on("-").splitToList(rule.getValue());
                if (dates.size() > 1) {
                    params.put("should_before", dates.get(0));
                    params.put("should_after", dates.get(1));
                } else {
                    if (AttributeRelationEnum.AFTER.value().equals(rule.getRelation())) {
                        params.put("should_before", dates.get(0));
                    }
                    if (AttributeRelationEnum.BEFORE.value().equals(rule.getRelation())) {
                        params.put("should_after", dates.get(0));
                    }
                }
            }
            if (rule.getName().equals(AttributeEnum.BRAND.value())) {
                String bids = contractBids(rule.getValue());
                if (AttributeRelationEnum.IN.value().equals(rule.getRelation())) {
                    params.put("shouldNot_bids", bids);
                } else {
                    params.put("should_bids", bids);
                }
            }
        }
        if (mustNotAttrsVal.size() > 0) {
            params.put("should_attrs", Joiner.on("_").join(mustNotAttrsVal));
        }
        if (attrsVal.size() > 0) {
            params.put("shouldNot_attrs", Joiner.on("_").join(attrsVal));
        }
        return params;
    }


    private String contractBids(String value) {
        List<String> brandNames = Splitters.COMMA.splitToList(value);
        List<Long> bcid = Lists.newArrayList();
        for (String brand : brandNames) {
            Response<Brand> resp = brandReadService.findByName(brand);
            if (!resp.isSuccess()) {
                throw new JsonResponseException(resp.getError());
            }
            bcid.add(resp.getResult().getId());

        }
        return Joiners.COMMA.join(bcid);
    }
}
