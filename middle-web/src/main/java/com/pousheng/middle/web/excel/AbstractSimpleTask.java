/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.web.excel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 14:36<br/>
 */
@Slf4j
@Data
public abstract class AbstractSimpleTask implements Serializable, TaskBehaviour {
    private static final long serialVersionUID = -4656101842353727559L;

    /**
     * 描述任务的元信息
     */
    private Map<String, Object> metaInfo;

    /**
     * 停止标记位，通知任务应该自行停止
     */
    private boolean stop = false;
    /**
     * 停止信号量，停止后由 {@link TaskContainer} 将信号量更新为 1
     */
    private AtomicInteger signal = new AtomicInteger(0);

    /**
     * 在此实现耗时的业务逻辑，如果里面包含循环，应该在循环入口检查
     * {@link #alive()} 来判断任务是否被手动终止
     */
    @Override
    public abstract void start();

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
    }

    /**
     * 任务执异常的回调方法
     */
    @Override
    public void onError() {
    }

    /**
     * 任务执行完毕后的回调方法，在这里放一些不耗时的收尾动作
     */
    @Override
    public void postStop() {
    }

    /**
     * 将 {@link #stop} 标记为设置为 {@code true}，并立即返回，
     * 不能保证一定停止任务，停止行为由任务自定义。
     */
    public boolean stop() {
        this.stop = true;
        onStop();
        return true;
    }

    /**
     * 将 {@link #stop} 标记为设置为 {@code true}，并等待一段时间，
     * 如果任务成功停止则返回成功，否则超出等待时间后返回失败
     *
     * @param timeout 超时时间
     * @param unit    重试时间单位
     * @return {@code true} 停止成功，{@code false} 停止失败，超出等待时间
     * @throws InterruptedException
     */
    public boolean stop(long timeout, TimeUnit unit) throws InterruptedException {
        log.info("stop current task {}, with timeout {}ms", this, unit.toMillis(timeout));
        this.stop = true;
        onStop();

        long timeoutMillis = unit.toMillis(timeout);
        long start = System.currentTimeMillis();
        while (true) {
            if (timeout != 0 && System.currentTimeMillis() - start >= timeoutMillis) {
                return false;
            }

            if (signal.get() == 1) {
                return true;
            }
            Thread.sleep(100);
        }
    }

    /**
     * 停止成功后更新信号量 {@link #signal} 至1，一般由 {@link TaskContainer} 调用，
     * 也可以在停止后自行调用
     *
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    protected boolean signalStop() {
        if (!signal.compareAndSet(0, 1)) {
            log.warn("task cannot stop twice.");
            return false;
        }
        return true;
    }

    public abstract boolean equalsTo(Object other);

    public abstract Object getTaskKey();

    public abstract Long getTaskId();

    public boolean isStop() {
        return stop;
    }

    public boolean alive() {
        return !stop;
    }

    public Map<String, Object> getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(Map<String, Object> metaInfo) {
        this.metaInfo = metaInfo;
    }
}
