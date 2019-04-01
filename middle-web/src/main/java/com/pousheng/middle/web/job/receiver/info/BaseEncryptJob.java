package com.pousheng.middle.web.job.receiver.info;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseEncryptJob<T> {

    public static final int BATCH_SIZE=100;

    private static final int POOL_SIZE=4;

    /**
     * 执行
     */
    public void run(){

        synchronized (this) {

            ThreadPoolExecutor executor=new ThreadPoolExecutor(
                    POOL_SIZE,
                    POOL_SIZE,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new ThreadFactoryBuilder().setNameFormat(getThreadPoolNameFormat()).build());
            int pageNo = 1;

            int count = 0;
            do {
                Response<Paging<T>> orderList = queryPaging(pageNo,BATCH_SIZE);
                if (orderList.isSuccess()
                        && !orderList.getResult().isEmpty()) {
                    count = orderList.getResult().getData().size();
                    pageNo++;

                    orderList.getResult().getData().forEach(data -> {
                        executor.execute(()->{
                            updateReceiverInfo(data);
                        });
                    });
                }
            } while (count % BATCH_SIZE == 0 && count != 0);

            executor.shutdown();
        }

    }

    /**
     * 线程池名称格式
     * @return
     */
    protected abstract String getThreadPoolNameFormat();

    /**
     * 分页查询数据
     * @return
     */
    protected abstract Response<Paging<T>> queryPaging(Integer pageNo,Integer limit);

    /**
     * 更新数据
     * @param data
     */
    protected abstract void updateReceiverInfo(T data);




}
