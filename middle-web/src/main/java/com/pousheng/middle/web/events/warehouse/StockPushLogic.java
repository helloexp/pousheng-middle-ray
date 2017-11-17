package com.pousheng.middle.web.events.warehouse;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */

import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 中台推送库存日志处理逻辑
 */
@Slf4j
@Component
public class StockPushLogic {
    @Autowired
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;

    /*private final ExecutorService executorService;
    @Autowired
    public StockPushLogic(@Value("${index.queue.size: 500000}") int queueSize,
                       @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(queueSize), (new ThreadFactoryBuilder()).setNameFormat("stock-push-log-%d").build(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("stock push log {} is rejected", r);
                    }
                });
    }
*/
    public void insertstockPushLog(StockPushLog stockPushLog){
        try {
            Response<Long> response = middleStockPushLogWriteService.create(stockPushLog);
            if (!response.isSuccess()){
                log.error("create stockPushLog failed, caused by {}",response.getError());
            }
        }catch (Exception e){
            log.error("create stockPushLog failed,caused by {}",e.getMessage());
        }

    }


    public Response<Boolean> batchInsertStockPushLog(List<StockPushLog> stockPushLogs){

            try {
                for (StockPushLog stockPushLog:stockPushLogs){
                    Response<Long> response = middleStockPushLogWriteService.create(stockPushLog);
                    if (!response.isSuccess()){
                        log.error("create stockPushLog failed, caused by {}",response.getError());
                    }
                }
                return Response.ok(Boolean.TRUE);
            }catch (Exception e){
                log.error("create stockPushLog failed,caused by {}",e.getMessage());
                return Response.ok(Boolean.FALSE);
            }

    }
}
