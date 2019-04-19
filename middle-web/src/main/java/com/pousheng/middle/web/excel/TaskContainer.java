package com.pousheng.middle.web.excel;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.terminus.common.exception.ServiceException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 14:15<br/>
 */
@Slf4j
@Component
public class TaskContainer implements Runnable {
    /**
     * 当前有任务执行时，每隔指定间隔轮询是否执行完毕，以执行下一个任务
     * 单位为毫秒
     */
    @Getter
    @Setter
    private Integer waitWhenOccupied = 10_000;

    private boolean stop = false;

    private static LinkedBlockingQueue<AbstractSimpleTask> Q = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor CORE = newExecutor("core");
    private ThreadPoolExecutor EXECUTOR = newExecutor("executor");
    private AbstractSimpleTask currentTask = null;
    private boolean started = Boolean.FALSE;

    private ThreadPoolExecutor newExecutor(String type) {
        // 防止老化，一秒后杀死空闲线程
        ThreadPoolExecutor e = new ThreadPoolExecutor(0, 1,
                1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("excel-task-pool-" + type + "-%d").build());
        return e;
    }

    private void init() {
        synchronized (this) {
            if (CORE == null || CORE.isShutdown()) {
                CORE = newExecutor("core");
            }
            if (EXECUTOR == null || EXECUTOR.isShutdown()) {
                EXECUTOR = newExecutor("executor");
            }
        }
    }

    private void shutdownNow() {
        synchronized (this) {
            started = false;
            if (CORE != null && !CORE.isShutdown()) {
                log.info("[TASK-CONTAINER] shutting down executor CORE");
                CORE.shutdownNow();
            }
            if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
                log.info("[TASK-CONTAINER] shutting down executor EXECUTOR");
                EXECUTOR.shutdownNow();
            }
        }
    }

    public void submit(AbstractSimpleTask task) {
        log.info("[TASK-CONTAINER] submit new task: {}", task);
        Q.offer(task);
        if (log.isDebugEnabled()) {
            log.info("[TASK-CONTAINER] current q size: {}", Q.size());
        }
    }

    public void start() {
        synchronized (this) {
            // Container should never start twice!
            if (started) {
                throw new RuntimeException("Container should never start twice!");
            } else {
                started = true;
            }

            // use existing executor
            CORE.submit(this);
        }
    }

    @Override
    public void run() {
        while (alive()) {
            if (log.isDebugEnabled()) {
                log.info("[TASK-CONTAINER] current q size: {}", Q.size());
            }

            // if pool empty or executor occupied, wait a while
            if (Q.isEmpty() || currentTask != null) {
                if (log.isDebugEnabled()) {
                    log.info("[TASK-CONTAINER] task q empty or current task occupied, wait for {} seconds", waitWhenOccupied / 1000);
                }
                delay(waitWhenOccupied, TimeUnit.MILLISECONDS);
                continue;
            }

            // fetch a task
            currentTask = Q.poll();
            if (currentTask == null) {
                if (log.isDebugEnabled()) {
                    log.info("[TASK-CONTAINER] no more new task from pool, skip");
                }
                continue;
            }

            // submit task
            EXECUTOR.submit(() -> {
                try {
                    log.info("[TASK-CONTAINER] about to executing task {}", currentTask);
                    currentTask.preStart();
                    log.info("[TASK-CONTAINER] start executing task {}", currentTask);
                    currentTask.start();
                    log.info("[TASK-CONTAINER] finishing executing task {}", currentTask);
                    currentTask.postStop();
                } catch (Exception e) {
                    log.error("[TASK-CONTAINER] failed to process task {}, cause: {}", currentTask, Throwables.getStackTraceAsString(e));
                    currentTask.onError();
                } finally {
                    currentTask.signalStop();
                    currentTask = null;
                }
            });
        }
    }

    private void delay(int seconds) {
        delay(seconds, TimeUnit.SECONDS);
    }

    private void delay(int seconds, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(seconds));
        } catch (InterruptedException e) {
            log.error("[TASK-CONTAINER]  failed to pause seconds {}, cause: {}", seconds, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }

    /**
     * 尝试终止当前任务
     *
     * @param taskKey 任务 ID
     * @return true 停止任务成功，false 未找到任务
     */
    public boolean tryKill(Object taskKey) {
        if (currentTask == null) {
            return false;
        }

        if (currentTask.equalsTo(taskKey)) {
            return currentTask.stop();
        }
        return false;
    }

    /**
     * 尝试终止当前任务
     *
     * @param taskKey 任务 ID
     * @param timeout 等待几秒让任务正确结束
     * @return true 停止任务成功，若超时未完成，则强制停止任务，false 未找到任务
     */
    public boolean tryKill(Object taskKey, Long timeout) throws InterruptedException {
        return tryKill(taskKey, timeout, TimeUnit.SECONDS);
    }

    public boolean tryKill(Object taskKey, Long timeout, TimeUnit unit) throws InterruptedException {
        if (currentTask == null && Q.isEmpty()) {
            return false;
        }

        if (currentTask != null) {
            if (!doKill(currentTask, taskKey, timeout, unit)) {
                log.warn("[TASK-CONTAINER] kill task timeout, force restart executor now.");
                shutdownNow();
                init();
                start();
                log.info("[TASK-CONTAINER] force restart finished.");
                return false;
            }
            return true;
        }

        for (AbstractSimpleTask task : Q) {
            if (doKill(task, taskKey)) {
                Q.remove(task);
                return true;
            }
        }
        return false;
    }

    private boolean doKill(AbstractSimpleTask task, Object key) {
        if (task.equalsTo(key)) {
            return task.stop();
        }
        return false;
    }

    private boolean doKill(AbstractSimpleTask task, Object key, Long timeout, TimeUnit unit) throws InterruptedException {
        if (task.equalsTo(key)) {
            return task.stop(timeout, unit);
        }
        return false;
    }

    /**
     * 获取当前等待执行任务数量
     */
    public Integer taskCount() {
        return Q.size();
    }

    /**
     * 获取当前等待的任务
     *
     * @return
     */
    public AbstractSimpleTask[] getTasks() {
        return Q.toArray(new AbstractSimpleTask[0]);
    }

    /**
     * 停止当前容器
     */
    public void stop() {
        this.stop = true;
    }

    /**
     * 当前容器是否运行中
     */
    public boolean alive() {
        return !stop;
    }

    /**
     * 当前容器是否运被停止
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * 获取当前容器状态快照
     */
    public TaskReportDTO report() {
        // get waiting task meta info
        AbstractSimpleTask[] tasks = getTasks();
        Map[] taskMetas = new Map[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            taskMetas[i] = tasks[i].getMetaInfo();
        }

        // get current task meta info
        Map<String, Object> currentMeta;
        if (currentTask != null) {
            currentMeta = currentTask.getMetaInfo();
        } else {
            currentMeta = null;
        }

        // build task container report
        return new TaskReportDTO(currentMeta, taskMetas, EXECUTOR.getTaskCount());
    }

    /**
     * 用于测试
     */
    public void join() {
        log.info("[TASK-CONTAINER] join current thread, current task: {}, q size: {}", currentTask, Q.size());
        while (true) {
            if (EXECUTOR.getActiveCount() > 0 || Q.size() > 0) {
                delay(10, TimeUnit.MILLISECONDS);
            } else {
                break;
            }
        }
    }

    public boolean contains(Long id) {
        for (AbstractSimpleTask task : Q) {
            if (Objects.equals(task.getTaskId(), id)) {
                return true;
            }
        }
        return false;
    }
}
