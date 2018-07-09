package com.pousheng.middle.web.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.middle.common.utils.component.SkutemplateScrollSearcher;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupSkuWriteService;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskReadService;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.utils.TaskTransUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.Pagination;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/11
 */

@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "true", matchIfMissing = false)
@Slf4j
@RestController
public class ItemGroupJobs {

    @RpcConsumer
    private ItemGroupReadService itemGroupReadService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private ScheduleTaskReadService scheduleTaskReadService;

    @RpcConsumer
    private ScheduleTaskWriteService scheduleTaskWriteService;

    @Autowired
    private SkutemplateScrollSearcher scrollSearcher;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @RpcConsumer
    private SkuTemplateDumpService skuTemplateDumpService;

    @RpcConsumer
    private ItemGroupSkuWriteService itemGroupSkuWriteService;

    @Autowired
    MaterialPusher materialPusher;

    private static final Integer BATCH_SIZE = 100;

    @Autowired
    private HostLeader hostLeader;

    /**
     * 每5分钟触发一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void synchronizeSpu() {
        log.info("JOB -- start to auto item group sku");
        ScheduleTask scheduleTask = findInitTask();
        while (scheduleTask != null) {
            Response<Boolean> updateResp = scheduleTaskWriteService.updateStatus(scheduleTask,TaskStatusEnum.EXECUTING.value());
            if (!updateResp.isSuccess()) {
                log.error("JOB -- finish to auto item group sku error, cause :{} ", updateResp.getError());
                throw new JsonResponseException(updateResp.getError());
            }

            onBatchHandleGroup(scheduleTask);
            scheduleTask = findInitTask();
        }
        log.info("JOB -- finish to auto item group sku");
    }

    private ScheduleTask findInitTask() {
        Response<ScheduleTask> resp = scheduleTaskReadService.findFirstByTypeAndStatus(
                TaskTypeEnum.ITEM_GROUP.value(), TaskStatusEnum.INIT.value());
        if (!resp.isSuccess()) {
            log.error("JOB -- finish to auto item group sku error, cause :{} ", resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    private void onBatchHandleGroup(ScheduleTask scheduleTask) {
        try {
            ItemGroupTask task = TaskTransUtil.trans(scheduleTask.getExtraJson());
            if (task.getGroupId() == null || task.getMark() == null || task.getType() == null) {
                throw new JsonResponseException("params.is.error");
            }
            int pageNo = 1;
            List<Long> skuTemplateIds = Lists.newArrayList();
            Set<String> materialIds = Sets.newHashSet();
            Map<String, String> params = task.getParams();
            if (params == null) {
                params = new HashMap<>();
            }
            Boolean mark = task.getMark();
            Integer type = task.getType();
            Long groupId = task.getGroupId();
            if (mark) {
                params.put("mustNot_groupId", groupId.toString());
            } else {
                if (type.equals(PsItemGroupSkuType.GROUP.value())) {
                    params.put("groupId", groupId.toString());
                }
                if (type.equals(PsItemGroupSkuType.EXCLUDE.value())) {
                    params.put("excludeGroupId", groupId.toString());
                }
            }
            String contextId = String.valueOf(DateTime.now().getMillis());
            log.info("async handle item group task start......");

            boolean next = batchHandleGroup(pageNo, BATCH_SIZE, params, contextId, skuTemplateIds, materialIds);
            while (next) {
                pageNo++;
                next = batchHandleGroup(pageNo, BATCH_SIZE, params, contextId, skuTemplateIds, materialIds);
                log.info("async handle item group sku " + pageNo * 100);
                if (pageNo % 10 == 0) {
                    Response<List<SkuTemplate>> listRes = skuTemplateReadService.findByIds(skuTemplateIds);
                    if (!listRes.isSuccess()) {
                        log.error("find sku template by ids:{} fail,error:{}", skuTemplateIds, listRes.getError());
                        continue;
                    }
                    //批量添加或删除映射关
                    batchMakeGroup(skuTemplateIds, mark, type, groupId);
                    skuTemplateDumpService.batchGroupDump(listRes.getResult());
                    //通知恒康关注商品库存
                    batchNoticeHk(materialIds, mark, type);
                    skuTemplateIds.clear();
                    materialIds.clear();
                }
            }
            //非1000条的更新下
            if (!CollectionUtils.isEmpty(skuTemplateIds)) {
                //批量更新es
                Response<List<SkuTemplate>> listRes = skuTemplateReadService.findByIds(skuTemplateIds);
                if (!listRes.isSuccess()) {
                    log.error("find sku template by ids:{} fail,error:{}", skuTemplateIds, listRes.getError());
                }
                //批量添加或删除映射关系
                batchMakeGroup(skuTemplateIds, mark, type, groupId);
                skuTemplateDumpService.batchGroupDump(listRes.getResult());
                batchNoticeHk(materialIds, mark, type);

            }
            scheduleTask.setStatus(TaskStatusEnum.FINISH.value());
            scheduleTaskWriteService.update(scheduleTask);
            log.info("async handle item group task end......");
        } catch (Exception e) {
            scheduleTask.setStatus(TaskStatusEnum.ERROR.value());
            scheduleTaskWriteService.update(scheduleTask);
            log.error("async handle item group task error", Throwables.getStackTraceAsString(e));
        }
    }


    /**
     * 分批次查询货品并进行处理
     *
     * @param pageNo 第几页
     * @param size   分页大小
     * @param params 查询参数
     * @return 是否更新成功
     */
    private Boolean batchHandleGroup(int pageNo, int size, Map<String, String> params, String contextId, List<Long> skuTemplateIds, Set<String> materialIds) {
        String templateName = "ps_search.mustache";
        Response<? extends Pagination<SearchSkuTemplate>> response = scrollSearcher
                .searchWithScroll(contextId, pageNo, size, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("fail to batch handle item group，param={},cause:{}", params, response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        if (response.getResult().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)) {
            return Boolean.FALSE;
        }
        skuTemplateIds.addAll(searchSkuTemplates.stream().map(SearchSkuTemplate::getId).collect(Collectors.toList()));
        materialIds.addAll(searchSkuTemplates.stream().map(SearchSkuTemplate::getSpuCode).collect(Collectors.toSet()));
        int current = searchSkuTemplates.size();
        return current == size;
    }

    private void batchMakeGroup(List<Long> skuTemplateIds, Boolean mark, Integer type, Long groupId) {
        if (mark) {
            itemGroupSkuWriteService.batchCreate(skuTemplateIds, groupId, type);
        } else {
            itemGroupSkuWriteService.batchDelete(skuTemplateIds, groupId, type);
        }
    }

    private void batchNoticeHk(Set<String> materialIds, Boolean mark, Integer type) {
        try {
            if (mark&&PsItemGroupSkuType.GROUP.value().equals(type)) {
                materialPusher.addMaterialIds(Lists.newArrayList(materialIds));
            }
            if (!mark) {
                String templateName = "ps_search.mustache";
                Map<String, String> params = new HashMap<>();
                params.put("materialIds", Joiners.COMMA.join(materialIds));
                Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 1000, templateName, params, SearchSkuTemplate.class);
                if (!response.isSuccess()) {
                    log.error("query sku template by materialIds:{} fail,error:{}", materialIds, response.getError());
                    throw new JsonResponseException(response.getError());
                }

                List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
                materialIds = searchSkuTemplates.stream().filter(e -> CollectionUtils.isEmpty(e.getGroupIds())).map(SearchSkuTemplate::getSkuCode).collect(Collectors.toSet());
                materialPusher.removeMaterialIds(Lists.newArrayList(materialIds));
            }

        } catch (Exception e) {
            if (mark) {
                log.info("add material from erp ,materialIds {}", materialIds);
            } else {
                log.info("remove material from erp ,materialIds {}", materialIds);
            }

        }

    }

}

