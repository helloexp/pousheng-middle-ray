package com.pousheng.middle.web.job;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.middle.common.utils.batchhandle.AbnormalRecord;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupSkuWriteService;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsItemGroupSkuMark;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskReadService;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import com.pousheng.middle.web.utils.TaskTransUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/11
 */

@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "true", matchIfMissing = false)
@Slf4j
@RestController
public class ItemGroupImportJobs {

    @RpcConsumer
    private ItemGroupReadService itemGroupReadService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private ScheduleTaskReadService scheduleTaskReadService;

    @RpcConsumer
    private ScheduleTaskWriteService scheduleTaskWriteService;

    @RpcConsumer
    private SkuTemplateDumpService skuTemplateDumpService;

    @RpcConsumer
    private ItemGroupSkuWriteService itemGroupSkuWriteService;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    @Autowired
    private MaterialPusher materialPusher;

    private static final String DEFAULT_CLOUD_PATH = "export";


    /**
     * 每5分钟触发一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void synchronizeSpu() {
        log.info("JOB -- start to import item group sku");
        ScheduleTask scheduleTask = findInitTask();
        while (scheduleTask != null) {
            Response<Boolean> updateResp = scheduleTaskWriteService.updateStatus(scheduleTask, TaskStatusEnum.EXECUTING.value());
            if (!updateResp.isSuccess()) {
                log.error("JOB -- finish to auto item group sku error, cause :{} ", updateResp.getError());
                throw new JsonResponseException(updateResp.getError());
            }
            if (updateResp.getResult()) {
                onBatchHandleGroupImport(scheduleTask);
            }
            scheduleTask = findInitTask();
        }

        log.info("JOB -- finish to  import item group sku");

    }

    private ScheduleTask findInitTask() {
        Response<ScheduleTask> resp = scheduleTaskReadService.findFirstByTypeAndStatus(
                TaskTypeEnum.ITEM_GROUP_IMPORT.value(), TaskStatusEnum.INIT.value());
        if (!resp.isSuccess()) {
            log.error("JOB -- finish to auto item group sku error, cause :{} ", resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    private void onBatchHandleGroupImport(ScheduleTask scheduleTask) {
        try {
            ItemGroupTask task = TaskTransUtil.trans(scheduleTask.getExtraJson());
            if (task.getGroupId() == null || task.getType() == null || StringUtils.isEmpty(task.getFileUrl())) {
                throw new JsonResponseException("params.is.error");
            }
            log.info("async import item group sku task start");
            String fileUrl = task.getFileUrl();
            Long groupId = task.getGroupId();
            Integer type = task.getType();
            ExcelExportHelper<AbnormalRecord> helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
            List<String[]> list = HandlerFileUtil.getInstance().handle(fileUrl);
            List<SkuTemplate> skuTemplates = Lists.newArrayList();
            for (int i = 1; i < list.size(); i++) {
                String[] strs = list.get(i);
                if (!Strings.isNullOrEmpty(strs[3]) && !"\"\"".equals(strs[3])) {
                    //sku编码
                    String skuCode = strs[3].replace("\"", "");
                    try {
                        SearchSkuTemplate searchSkuTemplate = findSkuTemplate(skuCode);
                        //不存在记录日志
                        if (Arguments.isNull(searchSkuTemplate)) {
                            appendErrorToExcel(helper, strs, "加入分组失败：不存在该商品");
                            log.error("import make sku code:{} flag fail, error:{}", skuCode, "不存在该商品");
                            continue;
                        }
                        if (searchSkuTemplate.getGroupIds() == null) {
                            searchSkuTemplate.setGroupIds(Sets.newHashSet());
                        }
                        if (searchSkuTemplate.getExcludeGroupIds() == null) {
                            searchSkuTemplate.setExcludeGroupIds(Sets.newHashSet());
                        }
                        Long skuTemplateId = searchSkuTemplate.getId();
                        if (PsItemGroupSkuType.EXCLUDE.value().equals(type)
                                && searchSkuTemplate.getExcludeGroupIds().contains(groupId)) {
                            continue;
                        }
                        if (PsItemGroupSkuType.GROUP.value().equals(type)
                                && searchSkuTemplate.getGroupIds().contains(groupId)) {
                            continue;
                        }
                        //判断商品是否有效
                        Response<SkuTemplate> skuTemplateRes = skuTemplateReadService.findById(skuTemplateId);
                        if (!skuTemplateRes.isSuccess()) {
                            log.error("find sku template by id:{} fail,error:{}", skuTemplateId, skuTemplateRes.getError());
                            appendErrorToExcel(helper, strs, "加入分组失败：" + skuTemplateRes.getError());
                            continue;
                        }
                        //判断商品是否被排除在外
                        if (PsItemGroupSkuType.GROUP.value().equals(type)
                                && searchSkuTemplate.getExcludeGroupIds().contains(groupId)) {
                            appendErrorToExcel(helper, strs, "加入分组失败：该商品为该组的排除商品");
                            continue;
                        }
                        //判断商品是否已在组内
                        if (PsItemGroupSkuType.EXCLUDE.value().equals(type)
                                && searchSkuTemplate.getGroupIds().contains(groupId)) {
                            appendErrorToExcel(helper, strs, "加入排除商品失败：该商品已在分组内");
                            continue;
                        }
                        skuTemplates.add(skuTemplateRes.getResult());
                        //每1000条更新下mysql和search
                        if (i % 1000 == 0) {
                            //批量添加或删除映射关系
                            List<String> skuCodes = skuTemplates.stream()
                                    .map(SkuTemplate::getSkuCode).collect(Collectors.toList());
                            itemGroupSkuWriteService.batchCreate(skuCodes, groupId, type, PsItemGroupSkuMark.ARTIFICIAL.value());
                            //批量更新es
                            skuTemplateDumpService.batchGroupDump(skuTemplates);
                            if (PsItemGroupSkuType.GROUP.value().equals(type)) {
                                batchNoticeHk(skuTemplates);
                            }
                            skuTemplates.clear();
                        }
                    } catch (Exception jre) {
                        appendErrorToExcel(helper, strs, "处理失败");
                        log.error("import make item group sku code:{} flag fail, cause:{}",
                                skuCode, Throwables.getStackTraceAsString(jre));
                    }
                }
            }
            //非1000条的更新下
            if (!CollectionUtils.isEmpty(skuTemplates)) {
                //批量添加或删除映射关系
                List<String> skuCodes = skuTemplates.stream().map(SkuTemplate::getSkuCode).collect(Collectors.toList());
                itemGroupSkuWriteService.batchCreate(skuCodes, groupId, type, PsItemGroupSkuMark.ARTIFICIAL.value());
                //批量更新es
                skuTemplateDumpService.batchGroupDump(skuTemplates);
                if (PsItemGroupSkuType.GROUP.value().equals(type)) {
                    batchNoticeHk(skuTemplates);
                }
            }
            if (helper.size() > 0) {
                String url = this.uploadToAzureOSS(helper.transformToFile());
                scheduleTask.setResult(url);
                log.error("async item group sku task abnormality");
            }
            scheduleTask.setStatus(TaskStatusEnum.FINISH.value());
            scheduleTaskWriteService.update(scheduleTask);
            log.info("async import item group sku task end");
        } catch (Exception e) {
            scheduleTask.setStatus(TaskStatusEnum.ERROR.value());
            scheduleTaskWriteService.update(scheduleTask);
            log.error("async handle item group task error", Throwables.getStackTraceAsString(e));
        }
    }

    private void batchNoticeHk(List<SkuTemplate> skuTemplates) {
        Set<String> materialIds = Sets.newHashSet();
        for (SkuTemplate sku : skuTemplates) {
            if (!StringUtils.isEmpty(sku.getExtra().get("materialId"))) {
                materialIds.add(sku.getExtra().get("materialId"));
            } else {
                log.info("sku template without materialId ,skuCode:{}", sku.getSkuCode());
            }
        }
        try {
            materialPusher.addMaterialIds(Lists.newArrayList(materialIds));
        } catch (Exception e) {
            log.info("add material from erp ,materialIds {}", materialIds);
        }
    }

    private SearchSkuTemplate findSkuTemplate(String skuCode) {
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCode", skuCode);
        Response<WithAggregations<SearchSkuTemplate>> response =
                skuTemplateSearchReadService.doSearchWithAggs(1, 20, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by skuCode:{} fail,error:{}", skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        if (CollectionUtils.isEmpty(searchSkuTemplates)) {
            return null;
        }
        return searchSkuTemplates.get(0);
    }

    private void appendErrorToExcel(ExcelExportHelper<AbnormalRecord> helper, String[] strs, String error) {
        AbnormalRecord abnormalRecord = new AbnormalRecord();
        abnormalRecord.setCode(strs[0].replace("\"", ""));
        abnormalRecord.setSize(strs[1].replace("\"", ""));
        abnormalRecord.setName(strs[2].replace("\"", ""));
        abnormalRecord.setSkuCode(strs[3].replace("\"", ""));
        abnormalRecord.setReason(error);
        helper.appendToExcel(abnormalRecord);
    }


    /**
     * 文件上传至微软云
     *
     * @param file 文件
     * @return 文件url
     */
    private String uploadToAzureOSS(File file) {
        String url;
        try {
            url = azureOssBlobClient.upload(file, DEFAULT_CLOUD_PATH);
            log.info("the azure blob url:{}", url);
            log.info("delete local file:{}", file.getPath());
            if (!file.delete()) {
                log.warn("delete local file fail:{}", file.getPath());
            }
        } catch (Exception e) {
            log.error(" fail upload file {} to azure,cause:{}", file.getName(), Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("fail upload file to azure");
        }
        return url;
    }
}

