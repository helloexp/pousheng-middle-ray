package com.pousheng.middle.web.async.supplyRule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.web.async.AsyncTask;
import com.pousheng.middle.web.async.TaskBehaviour;
import com.pousheng.middle.web.async.TaskResponse;
import io.terminus.common.model.Response;
import lombok.Setter;
import org.joda.time.DateTime;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/5
 */
public class SkuSupplyRuleDisableTask implements AsyncTask, TaskBehaviour {
    @Setter
    private Long shopId;
    @Setter
    private Long brandId;

    private Long taskId;

    private SkuSupplyRuleDisableParser skuSupplyRuleDisableParser;

    public static SkuSupplyRuleDisableTask newInstance(Long shopId, Long brandId) {
        SkuSupplyRuleDisableTask instance = new SkuSupplyRuleDisableTask();
        instance.setShopId(shopId);
        instance.setBrandId(brandId);
        return instance;
    }

    /**
     * 初始
     */
    @Override
    public Response<Long> init() {
        skuSupplyRuleDisableParser = SupplyRuleParserFactory.get(shopId, brandId);
        Response<Long> init = skuSupplyRuleDisableParser.init();
        taskId = init.getResult();
        return init;
    }

    /**
     * 这里判断任务是否需要停止，如是否超时
     */
    @Override
    public Boolean needStop() {
        TaskResponse lastStatus = skuSupplyRuleDisableParser.getLastStatus();
        return TaskStatusEnum.EXECUTING.name().equals(lastStatus.getStatus()) && new DateTime(lastStatus.getInitDate()).plusHours(1).isBeforeNow();
    }

    /**
     * 任务id
     */
    @Override
    public Long getTaskId() {
        return taskId;
    }

    /**
     * 任务类型
     *
     * @see TaskTypeEnum
     */
    @Override
    public String getTaskType() {
        return TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE.name();
    }

    /**
     * 执行状态 todo 删除
     */
    @Override
    public TaskResponse getLastStatus(String taskType) {

        return skuSupplyRuleDisableParser.getLastStatus();
    }

    @Override
    public ThreadPoolExecutor getTaskExecutor() {
        return new ThreadPoolExecutor(0 ,
                1,
                1L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadFactoryBuilder().setNameFormat("sku-supply-disable-task-pool-%d").build());
    }

    /**
     * 在此实现耗时的业务逻辑，如果里面包含循环，应该在循环入口检查
     */
    @Override
    public void start() {
        skuSupplyRuleDisableParser.start();
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
        skuSupplyRuleDisableParser.onStop();
    }

    /**
     * 任务执异常的回调方法
     */
    @Override
    public void onError(Exception e) {
        skuSupplyRuleDisableParser.onError(e);
    }

    /**
     * 任务执行完毕后的回调方法，在这里放一些不耗时的收尾动作
     */
    @Override
    public void manualStop() {
        skuSupplyRuleDisableParser.manualStop();
    }

    @Override
    public AsyncTask getTask(TaskDTO task) {
        shopId = (Long) task.getContent().get("shopId");
        brandId = (Long) task.getContent().get("brandId");
        taskId = task.getId();
        skuSupplyRuleDisableParser = SupplyRuleParserFactory.get(shopId, brandId);
        return this;
    }

    @Override
    public TaskResponse getLastStatus() {
        return skuSupplyRuleDisableParser.getLastStatus();
    }
}
