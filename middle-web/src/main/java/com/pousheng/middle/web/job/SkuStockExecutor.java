package com.pousheng.middle.web.job;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SkuStockExecutor {

    private final ExecutorService executorService;

    @Autowired
    public SkuStockExecutor(@Value("${index.queue.size: 500000}")int queueSize) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 4, Runtime.getRuntime().availableProcessors() * 6, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("stock-pusher-%d").build(),
                (r, executor) -> log.error("stock push task {} is rejected", r));
    }

    public void submit(Runnable r) {
        this.executorService.submit(r);
    }

    @PreDestroy
    public void shutdown(){
        this.executorService.shutdown();
    }

}
