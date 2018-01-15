package com.pousheng.middle.web.order.job;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.open.mpos.MposOrderHandleLogic;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.model.Paging;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by ph on 2018/1/10
 * 定时拉取Mpos订单跟踪发货单状态
 */
@Slf4j
@Component
@RestController
public class MposJob {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private MposOrderHandleLogic orderHandleLogic;

    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    private final ExecutorService executorService;

    @Value("${open.client.sync.all.order.duration.in.minute:3000}")
    private Integer syncAllOrderDurationInMinute;

    @Value("${open.client.sync.order.fetch.size:200}")
    private Integer shipmentFetchSize;

    @Autowired
    public MposJob(@Value("${order.queue.size: 2000}") int queueSizeOfOrder){
        this.executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(queueSizeOfOrder),
                new ThreadFactoryBuilder().setNameFormat("mpos-order-fetcher-%d").build(),
                (r, executor) -> log.error("task {} is rejected", r));
    }

    /**
     * 每隔5分钟拉取一次mpos发货单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncMposShipment() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        DateTime now = DateTime.now();
        DateTime startAt = now.minusMinutes(syncAllOrderDurationInMinute);
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("start to sync mpos shipment...");
            int pageNo = 1;
            while (true) {
                Paging<MposShipmentExtra> pagination = syncMposShipmentLogic.syncMposShimentStatus(pageNo,shipmentFetchSize,startAt.toDate(),now.toDate());
                final List<MposShipmentExtra> mposShipmentExtras = pagination.getData();
                if (CollectionUtils.isEmpty(mposShipmentExtras)) {
                    break;
                }
                //异步处理
                executorService.submit(new OrderHandleTask(mposShipmentExtras));
                if (!Objects.equals(mposShipmentExtras.size(),shipmentFetchSize)) {
                    break;
                }
                pageNo++;
                break;
            }
        stopwatch.stop();
        log.info("end to sync mpos shipment,and cost {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));
    }

    @RequestMapping(value = "api/mpos/sync/shipment/job",method = RequestMethod.GET)
    public void test(){
        this.syncMposShipment();
    }


    private class OrderHandleTask implements Runnable{

        private final List<MposShipmentExtra> mposShipmentExtras;

        public OrderHandleTask(List<MposShipmentExtra> mposShipmentExtras){
            this.mposShipmentExtras = mposShipmentExtras;
        }

        @Override
        public void run() {
            orderHandleLogic.handleOrder(mposShipmentExtras);
        }
    }

    @PreDestroy
    public void shutdown() {
        this.executorService.shutdown();
    }

}
