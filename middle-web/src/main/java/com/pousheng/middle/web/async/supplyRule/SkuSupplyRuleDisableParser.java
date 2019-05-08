package com.pousheng.middle.web.async.supplyRule;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.api.CreateTaskRequest;
import com.pousheng.middle.task.api.PagingTaskRequest;
import com.pousheng.middle.task.api.QuerySingleTaskByIdRequest;
import com.pousheng.middle.task.api.UpdateTaskRequest;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.async.TaskBehaviour;
import com.pousheng.middle.web.async.TaskResponse;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/6
 */
@Slf4j
public class SkuSupplyRuleDisableParser implements TaskBehaviour {

    private Long taskId;

    private Long shopId;

    private Long brandId;

    private Long count = 0L;

    private final TaskWriteFacade taskWriteFacade;

    private final TaskReadFacade taskReadFacade;

    private final PoushengMiddleSpuService poushengMiddleSpuService;

    private final ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;

    private final SkuTemplateSearchReadService skuTemplateSearchReadService;

    private JedisTemplate jedisTemplate;

    public SkuSupplyRuleDisableParser(Long shopId,
                                      Long brandId,
                                      TaskWriteFacade taskWriteFacade,
                                      TaskReadFacade taskReadFacade,
                                      PoushengMiddleSpuService poushengMiddleSpuService,
                                      ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent,
                                      JedisTemplate jedisTemplate,
                                      SkuTemplateSearchReadService skuTemplateSearchReadService) {
        this.shopId = shopId;
        this.brandId = brandId;
        this.taskWriteFacade = taskWriteFacade;
        this.taskReadFacade = taskReadFacade;
        this.poushengMiddleSpuService = poushengMiddleSpuService;
        this.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
        this.jedisTemplate = jedisTemplate;
        this.skuTemplateSearchReadService = skuTemplateSearchReadService;
    }

    /**
     * 初始
     */
    @Override
    public Response<Long> init() {
        try {
            Long currentTaskId = getCurrentTaskId();
            if (currentTaskId > 0) {
                taskId = currentTaskId;
                QuerySingleTaskByIdRequest request = new QuerySingleTaskByIdRequest();
                request.setTaskId(taskId);
                TaskDTO taskDTO = taskReadFacade.querySingleTaskById(request).getResult();
                if (taskDTO != null && TaskStatusEnum.EXECUTING.name().equals(taskDTO.getStatus())) {
                    return Response.fail("sku.supply.rule.disable.handling");
                }
            }
            createTask();
            setCurrentTaskId(taskId);
            return Response.ok(taskId);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 在此实现耗时的业务逻辑，如果里面包含循环，应该在循环入口检查
     */
    @Override
    public void start() {
        updateTaskStatus(TaskStatusEnum.EXECUTING);
        //限制规则操作范围
        Response<Long> ruleResp = shopSkuSupplyRuleComponent.queryTopSupplyRuleId();
        if (!ruleResp.isSuccess()) {
            throw new ServiceException(ruleResp.getError());
        }

        Integer spuPageNo = 1;
        Integer spuPageSize = 100;
        Map<String, Object> params = Maps.newHashMap();
        params.put("brandId", brandId);
        Response<Paging<Spu>> spuPaging = poushengMiddleSpuService.findBy(spuPageNo, spuPageSize, params);
        if (!spuPaging.isSuccess()) {
            throw new ServiceException(spuPaging.getError());
        }
        if (spuPaging.getResult().isEmpty()) {
            onStop();
            return;
        }

        //根据库存服务数量设置并行，保留一个线程给主线程，最大4个并行
        ThreadPoolExecutor CORE = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1), new ThreadFactoryBuilder().setNameFormat("sku-supply-disable-handle-pool-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        int skuPageNo = 1;
        int skuPageSize = 2000;
        List<Spu> spuList = spuPaging.getResult().getData();
        List<Long> spuIds = spuList.stream().map(Spu::getId).collect(Collectors.toList());
        SkuSupplyRuleDisableHandle handle = new SkuSupplyRuleDisableHandle(shopId, ruleResp.getResult(), shopSkuSupplyRuleComponent);

        while (!spuList.isEmpty()) {
            if (getCurrentTaskId() <= 0) {
                //不会中断已执行部分
                throw new ServiceException("sku.supply.rule.disable.stopped");
            }
            Response<Paging<SkuTemplate>> skuPaging = skuTemplateSearchReadService.pagingSkuBySpuIds(skuPageNo, skuPageSize, spuIds);
            if (!skuPaging.isSuccess()) {
                throw new ServiceException(skuPaging.getError());
            }
            while (!skuPaging.getResult().isEmpty()) {
                for (SkuTemplate skuTemplate : skuPaging.getResult().getData()) {
                    if (handle.isFull()) {//每个线程最大2000个sku
                        log.info("[sku-supply-rule-disable-handle-start] CORE:{}", CORE);
                        CORE.submit(handle);
                        handle = new SkuSupplyRuleDisableHandle(shopId, ruleResp.getResult(), shopSkuSupplyRuleComponent);
                    }

                    handle.append(skuTemplate.getSkuCode());
                    count++;
                }
                skuPageNo++;
                skuPaging = skuTemplateSearchReadService.pagingSkuBySpuIds(skuPageNo, skuPageSize, spuIds);
                if (!skuPaging.isSuccess()) {
                    throw new ServiceException(skuPaging.getError());
                }
            }
            skuPageNo = 1;
            spuPageNo++;
            spuPaging = poushengMiddleSpuService.findBy(spuPageNo, spuPageSize, params);
            if (!spuPaging.isSuccess()) {
                throw new ServiceException(spuPaging.getError());
            }
            spuList = spuPaging.getResult().getData();
        }

        if (handle.size() > 1) {
            CORE.submit(handle);
        }

    }

    /**
     * 任务准备执行时的回调方法，让任务准备前置数据
     */
    @Override
    public void preStart() {

    }

    /**
     * 任务执行完毕时的回调方法，让任务感知到被中止了
     */
    @Override
    public void onStop() {
        log.info("[sku-supply-rule-disable-parser] executed skuCode count:{}", count);
        setCurrentTaskId(0L);
        updateTaskStatus(TaskStatusEnum.FINISH);
    }

    /**
     * 任务执异常的回调方法
     */
    @Override
    public void onError(Exception e) {
        log.info("[sku-supply-rule-disable-parser] executed skuCode count:{}", count);
        setCurrentTaskId(0L);
        updateTaskStatus(TaskStatusEnum.ERROR, e);
    }

    /**
     * 任务执行完毕后的回调方法，在这里放一些不耗时的收尾动作
     */
    @Override
    public void manualStop() {
        setCurrentTaskId(0L);
        updateTaskStatus(TaskStatusEnum.STOPPED);
    }

    @Override
    public TaskResponse getLastStatus() {
        Long currentTaskId = getCurrentTaskId();
        TaskDTO taskDTO = null;
        if (currentTaskId > 0) {
            QuerySingleTaskByIdRequest request = new QuerySingleTaskByIdRequest();
            request.setTaskId(currentTaskId);
            taskDTO = taskReadFacade.querySingleTaskById(request).getResult();
        } else {
            PagingTaskRequest request = new PagingTaskRequest();
            request.setType(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name());
            request.setPageSize(1);
            Paging<TaskDTO> result = taskReadFacade.pagingTasks(request).getResult();
            if (result.isEmpty()) {
                return TaskResponse.empty();
            }
            taskDTO = result.getData().get(0);
        }

        TaskResponse response = new TaskResponse();
        response.setInitDate(taskDTO.getCreatedAt());
        response.setStatus(taskDTO.getStatus());
        response.setType(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name());
        response.setContent(taskDTO.getContent());
        return response;
    }

    private Long createTask() {
        Map<String, Object> content = Maps.newHashMap();
        content.put("shopId", shopId);
        content.put("brandId", brandId);
        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setType(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name());
        createTaskRequest.setStatus(TaskStatusEnum.INIT.name());
        createTaskRequest.setContent(content);
        Response<Long> taskResp = taskWriteFacade.createTask(createTaskRequest);
        if (!taskResp.isSuccess()) {
            log.error("failed to create async task record {}, cause: {}", createTaskRequest, taskResp.getError());
            throw new ServiceException(taskResp.getError());
        }

        this.taskId = taskResp.getResult();
        log.info("[SKU-SUPPLY-RULE-DISABLE-PARSER] create task {}", taskId);
        return taskResp.getResult();
    }

    private void updateTaskStatus(TaskStatusEnum statusEnum) {
        log.info("[SKU-SUPPLY-RULE-DISABLE-PARSER] update task {} status to {}", taskId, statusEnum);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTaskId(taskId);
        request.setStatus(statusEnum.name());
        Map<String, Object> content = Maps.newHashMap();
        content.put("count", count);
        request.setContent(content);
        Response<Boolean> r = taskWriteFacade.updateTask(request);
        if (!r.isSuccess()) {
            log.error("failed to update task({}) status to {}, cause: {}", taskId, statusEnum, r.getError());
            throw new ServiceException(r.getError());
        }
    }

    private void updateTaskStatus(TaskStatusEnum statusEnum, Exception e) {
        log.info("[SKU-SUPPLY-RULE-DISABLE-PARSER] update task {} status to {}", taskId, statusEnum);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTaskId(taskId);
        request.setStatus(statusEnum.name());
        Map<String, Object> content = Maps.newHashMap();
        content.put("count", count);
        content.put("msg", e.getMessage());
        request.setContent(content);
        Response<Boolean> r = taskWriteFacade.updateTask(request);
        if (!r.isSuccess()) {
            log.error("failed to update task({}) status to {}, cause: {}", taskId, statusEnum, r.getError());
            throw new ServiceException(r.getError());
        }
    }

    private void setCurrentTaskId(Long taskId) {
        jedisTemplate.execute(jedis -> {
            jedis.set(getTaskKey(), taskId.toString());
        });
    }

    private Long getCurrentTaskId() {
        Long taskId = jedisTemplate.execute(jedis -> {
            String taskId1 = jedis.get(getTaskKey());
            return Strings.isNullOrEmpty(taskId1) ? 0L: Long.valueOf(taskId1);
        });
        return taskId;
    }

    private String getTaskKey() {
        return "async_task_"+ TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name();
    }

}
