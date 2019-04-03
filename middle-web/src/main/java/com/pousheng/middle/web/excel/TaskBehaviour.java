/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.web.excel;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-17 11:07<br/>
 */
public interface TaskBehaviour {
    boolean alive();

    /**
     * 在此实现耗时的业务逻辑，如果里面包含循环，应该在循环入口检查
     * {@link #alive()} 来判断任务是否被手动终止
     */
    void start();

    /**
     * 任务准备执行时的回调方法，让任务准备前置数据
     */
    void preStart();

    /**
     * 任务执行完毕时的回调方法，让任务感知到被中止了
     */
    void onStop();

    /**
     * 任务执异常的回调方法
     */
    void onError();

    /**
     * 任务执行完毕后的回调方法，在这里放一些不耗时的收尾动作
     */
    void postStop();
}
