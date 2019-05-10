/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.web.async;


import io.terminus.common.model.Response;

public interface TaskBehaviour {

    /**
     * 初始
     */
    Response<Long> init();

    /**
     * 在此实现耗时的业务逻辑，如果里面包含循环，应该在循环入口检查
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
    void onError(Exception e);

    /**
     * 任务执行完毕后的回调方法
     */
    void manualStop();

    TaskResponse getLastStatus();
}
