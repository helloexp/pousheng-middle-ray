package com.pousheng.middle.web.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.web.events.warehouse.StockPushLogic;
import com.pousheng.middle.web.redis.RedisQueueCustomer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Slf4j
//@Component
public class RealTimeIndexer {

    @Autowired
    private RedisQueueCustomer customer;
    @Autowired
    private StockPushLogic stockPushLogic;

    private static final TypeReference<List<StockPushLog>> LIST_OF_STOCK_PUSH_LOG = new TypeReference<List<StockPushLog>>() {};


    @PostConstruct
    public void doIndex() {
        new Thread(new IndexTask(customer, stockPushLogic)).start();
    }



    class IndexTask implements Runnable {

        private final RedisQueueCustomer customer;
        private final StockPushLogic stockPushLogic;

        public IndexTask(RedisQueueCustomer customer,
                         StockPushLogic stockPushLogic) {
            this.customer = customer;
            this.stockPushLogic = stockPushLogic;
        }


        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p/>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            log.info("start fetch index");
            while (true) {
                try {
                    String stockJson = customer.pop();
                    //弹出一个元素
                    if (!Strings.isNullOrEmpty(stockJson)) {
                        doProcess(stockJson);
                    } else {
                        Thread.sleep(100000);
                    }
                } catch (Exception e) {
                    log.warn("fail to process, cause:{}", Throwables.getStackTraceAsString(e));
                }
            }
        }
    }


    private void doProcess(String stockJson) {

        //转换为dto对象
        List<StockPushLog> stockPushLogs = readStockFromJson(stockJson);
        if (stockPushLogs.size()!=0){
            //2、新增日志
            Response<Boolean> updateRes = stockPushLogic.batchInsertStockPushLog(stockPushLogs);
            if(!updateRes.isSuccess()){
                log.error("insert stock push log :{} fail,error:{}",stockPushLogs,updateRes.getError());
                throw new OPServerException(200,updateRes.getError());
            }
        }

    }


    private List<StockPushLog> readStockFromJson(String json) {

        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(json, LIST_OF_STOCK_PUSH_LOG);
        } catch (IOException e) {
            log.error("analysis json:{} to stock dto error,cause:{}",json, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();

    }
}
