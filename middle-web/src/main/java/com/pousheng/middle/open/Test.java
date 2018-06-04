package com.pousheng.middle.open;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.warehouse.model.StockPushLog;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.concurrent.*;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/4
 * pousheng-middle
 */
public class Test {
    public static void main(String[] args) {
        final List<StockPushLog> thirdStockPushLogs = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            addStockLog(thirdStockPushLogs, Long.valueOf(i));
        }
        System.out.println(thirdStockPushLogs);
    }

    static ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 6, 60L, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000), (new ThreadFactoryBuilder()).setNameFormat("stock-push-%d").build(),
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

                }
            });

    public static void addStockLog(final List<StockPushLog> thirdStockPushLogs, Long quantity) {
        executorService.submit(() -> {
            StockPushLog stockPushLog = new StockPushLog();
            stockPushLog.setQuantity(quantity);
            thirdStockPushLogs.add(stockPushLog);
        });
    }
}
