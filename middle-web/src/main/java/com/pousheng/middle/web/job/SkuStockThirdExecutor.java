package com.pousheng.middle.web.job;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
@Slf4j
public class SkuStockThirdExecutor {

    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> blockingQueue;

    @Autowired
    public SkuStockThirdExecutor(@Value("${stock.task.queue.size: 50}")int queueSize,
                                 @Value("${stock.task.single.process.pool.core.size: 2}")int singleCorePoolSize,
                                 @Value("${stock.task.single.process.pool.max.size: 3}")int singleMaxPoolSize) {
        blockingQueue = new ArrayBlockingQueue<>(queueSize);
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * singleCorePoolSize, Runtime.getRuntime().availableProcessors() * singleMaxPoolSize, 60L, TimeUnit.MINUTES,
                blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("stock-pusher-%d").build(),
                (r, executor) -> log.error("stock push  third task {} is rejected", r));
    }

    public void submit(Runnable r) {
        this.executorService.submit(r);
    }

    @PreDestroy
    public void shutdown(){
        this.executorService.shutdown();
    }

    public int remainingCapacity(){
        return blockingQueue.remainingCapacity();
    }

}
