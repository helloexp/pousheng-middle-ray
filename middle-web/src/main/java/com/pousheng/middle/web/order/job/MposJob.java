package com.pousheng.middle.web.order.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.open.mpos.MposOrderHandleLogic;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationReadService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Shipment;
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
import java.util.Map;
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
    private AutoCompensationReadService autoCompensationReadService;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    private final ExecutorService executorService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Value("${open.client.sync.all.order.duration.in.minute:20}")
    private Integer syncAllOrderDurationInMinute;

    @Value("${open.client.sync.order.fetch.size:200}")
    private Integer shipmentFetchSize;

    @Autowired
    public MposJob(@Value("${shipment.queue.size: 20000}") int queueSizeOfOrder){
        this.executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(queueSizeOfOrder),
                new ThreadFactoryBuilder().setNameFormat("mpos-shipment-fetcher-%d").build(),
                (r, executor) -> log.error("task {} is rejected", r));
    }

    /**
     * 每隔3分钟拉取一次mpos发货单
     */
    @Scheduled(cron = "0 */3 * * * ?")
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

    /**
     * 自动同步失败任务
     */
    @Scheduled(cron = "0 */15 * * * ?")
    public void autoCompensateMposFailTask(){
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("start to compensate mpos not dispatcher sku...");

        Map<String,Object> param = Maps.newHashMap();
        param.put("status",0);
        int pageNo = 1;
        while (true) {
            Response<Paging<AutoCompensation>> response = autoCompensationReadService.pagination(pageNo,20,param);
            if(!response.isSuccess()){
                log.error("fail to find compensation task");
                return ;
            }
            Paging<AutoCompensation> pagination = response.getResult();
            final List<AutoCompensation> autoCompensations = pagination.getData();
            if (CollectionUtils.isEmpty(autoCompensations)) {
                break;
            }
            //异步处理
            executorService.submit(new CompensationTask(autoCompensations));
            if (!Objects.equals(autoCompensations.size(),shipmentFetchSize)) {
                break;
            }
            pageNo++;
            break;
        }

        stopwatch.stop();
        log.info("end to compensate not dispatcher sku to mpos,and cost {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));
    }

    @RequestMapping(value = "api/mpos/sync/shipment/job",method = RequestMethod.GET)
    public void syncMposShipmentBySelf(){
        this.syncMposShipment();
    }

    @RequestMapping(value = "api/mpos/compensate/job",method = RequestMethod.GET)
    public void autoCompensateMposFailTaskBySelf(){
        this.autoCompensateMposFailTask();
    }

    /**
     * 处理发货单状态更新任务
     */
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

    /**
     * 自动补偿任务
     */
    private class CompensationTask implements Runnable{
        private final List<AutoCompensation> autoCompensations;

        public CompensationTask(List<AutoCompensation> autoCompensations){
            this.autoCompensations = autoCompensations;
        }

        @Override
        public void run() {
            if(!CollectionUtils.isEmpty(autoCompensations)){
                autoCompensations.forEach(autoCompensation -> {
                    if(Objects.equals(autoCompensation.getType(), TradeConstants.FAIL_NOT_DISPATCHER_SKU_TO_MPOS)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Response<Boolean> response = syncMposOrderLogic.syncNotDispatcherSkuToMpos(mapper.fromJson(extra.get("param"),Map.class));
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_REFUND_RECEIVE_TO_MPOS)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Response<Boolean> response = syncMposOrderLogic.notifyMposRefundReceived(mapper.fromJson(extra.get("param"),Map.class));
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_SYNC_POS_TO_HK)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Map<String,Object> param = mapper.fromJson(extra.get("param"),Map.class);
                            Shipment shipment = shipmentReadLogic.findShipmentById((Long)param.get("shipmentId"));
                            Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Map<String,Object> param = mapper.fromJson(extra.get("param"),Map.class);
                            Shipment shipment = shipmentReadLogic.findShipmentById((Long)param.get("shipmentId"));
                            Response<Boolean> response = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }
                        }
                    }
                });
            }
        }

    }

    @PreDestroy
    public void shutdown() {
        this.executorService.shutdown();
    }

}
