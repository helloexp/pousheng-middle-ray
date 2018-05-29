package com.pousheng.middle.web.order.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.open.mpos.MposOrderHandleLogic;
import com.pousheng.middle.open.mpos.dto.MposShipmentExtra;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationReadService;
import com.pousheng.middle.warehouse.service.SkuStockTaskReadService;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.HKShipmentDoneLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import com.pousheng.middle.web.shop.event.UpdateShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.util.HashMap;
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
    private EventBus eventBus;

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

    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;

    @Autowired
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private ShopReadService shopReadService;

    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;

    @Autowired
    private SkuStockTaskWriteService skuStockTaskWriteService;

    private final ExecutorService executorService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Value("${open.client.sync.all.order.duration.in.minute:20}")
    private Integer syncAllOrderDurationInMinute;

    @Value("${open.client.sync.order.fetch.size:40}")
    private Integer shipmentFetchSize;

    @Value("${auto.task.try.number:10}")
    private Integer autoTryNumber;

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
    @Scheduled(cron = "0 */3 * * * ?")
    public void autoCompensateMposFailTask(){
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("start to compensate mpos not dispatcher sku...");

        Map<String,Object> param = Maps.newHashMap();
        param.put("status",0);
        param.put("time",autoTryNumber);
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




    @RequestMapping(value = "api/mpos/shop/address/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullDump() {  //每天凌晨2点执行一次
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("sync mpos shop address fired");
        int pageNo = 1;
        boolean next = batchSyncShopAddress(pageNo, 500);
        while (next) {
            pageNo ++;
            next = batchSyncShopAddress(pageNo, 500);
        }
        log.info("sync mpos shop address end");
    }

    /**
     * 每隔10分钟尝试把超时处理中的任务状态回滚到待处理
     */
    //@Scheduled(cron = "0 */10 * * * ?")
    public void compensationSkuStockTask() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("start to compensationSkuStockTask...");

        Response<Boolean> updateRes = skuStockTaskWriteService.updateTimeOutHandleTask();
        if (!updateRes.isSuccess()){
            log.error("updateTimeOutHandleTask fail,error:{}",updateRes.getError());
        }
    }


    @SuppressWarnings("unchecked")
    private boolean batchSyncShopAddress(int pageNo, int size) {

        //String name, Long userId, Integer type, Integer status, Integer pageNo, Integer pageSize
        Response<Paging<Shop>> pagingRes = shopReadService.pagination(null,null,null,1,pageNo, size);
        if(!pagingRes.isSuccess()){
            log.error("paging shop fail,error:{}",pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<Shop> paging = pagingRes.getResult();
        List<Shop> shops = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(shops)) {
            return Boolean.FALSE;
        }

        for (Shop shop : shops){
            UpdateShopEvent updateShopEvent = new UpdateShopEvent(shop.getId(),shop.getBusinessId(),shop.getOuterId());
            eventBus.post(updateShopEvent);
        }

        int current = shops.size();
        return current == size;  // 判断是否存在下一个要处理的批次
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
                            }else{
                                autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_REFUND_RECEIVE_TO_MPOS)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Response<Boolean> response = syncMposOrderLogic.notifyMposRefundReceived(mapper.fromJson(extra.get("param"),Map.class));
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }else{
                                autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_SYNC_POS_TO_HK)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Map<String,Long> param =  mapper.fromJson(extra.get("param"), mapper.createCollectionType(HashMap.class, String.class, Long.class));
                            Shipment shipment = shipmentReadLogic.findShipmentById(param.get("shipmentId"));
                            Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }else{
                                autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK)){
                        Map<String,String> extra = autoCompensation.getExtra();
                        if(Objects.nonNull(extra.get("param"))){
                            Map<String,Long> param =  mapper.fromJson(extra.get("param"), mapper.createCollectionType(HashMap.class, String.class, Long.class));
                            Shipment shipment = shipmentReadLogic.findShipmentById(param.get("shipmentId"));
                            Response<Boolean> response = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
                            if(response.isSuccess()){
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }else{
                                autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                            }
                        }
                    }
                    if(Objects.equals(autoCompensation.getType(),TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK)) {
                        Map<String, String> extra = autoCompensation.getExtra();
                        if (Objects.nonNull(extra.get("param"))) {
                            Map<String,Long> param =  mapper.fromJson(extra.get("param"), mapper.createCollectionType(HashMap.class, String.class, Long.class));
                            Refund refund = refundReadLogic.findRefundById(param.get("refundId"));
                            Response<Boolean> response = syncRefundPosLogic.syncRefundPosToHk(refund);
                            if (response.isSuccess()) {
                                autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
                            }else{
                                autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                            }
                        }
                    }

                    if(Objects.equals(autoCompensation.getType(),TradeConstants.YYEDI_SHIP_NOTIFICATION)){
                        Map<String, String> extra = autoCompensation.getExtra();
                        if (Objects.nonNull(extra.get("param"))) {
                            Map<String,Long> param =  mapper.fromJson(extra.get("param"), mapper.createCollectionType(HashMap.class, String.class, Long.class));
                            Long shipmentId = Long.valueOf(param.get(TradeConstants.SHIPMENT_ID));
                            log.info("try to sync shipment(id:{}) to hk",shipmentId);
                            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);

                            //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
                            hkShipmentDoneLogic.doneShipment(shipment);

                            //同步pos单到恒康
                            Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                            if (!response.isSuccess()) {
                                Map<String, Object> param1 = Maps.newHashMap();
                                param1.put("shipmentId", shipment.getId());
                                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
                            }
                            autoCompensateLogic.updateAutoCompensationTask(autoCompensation.getId());
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
