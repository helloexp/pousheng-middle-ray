package com.pousheng.middle.web.order.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationReadService;
import com.pousheng.middle.order.service.AutoCompensationWriteService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.HKShipmentDoneLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2018/6/12
 * 为了迎接618大促，先这么写，后期会改到另一个任务表里
 */
//@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
//@Component
@Slf4j
public class YyEdiShipAsyncJob extends AbstractAsyncJob {

    @Autowired
    private AutoCompensationReadService autoCompensationReadService;

    @Autowired
    private AutoCompensationWriteService autoCompensationWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    private final ExecutorService executorService;

    private final BlockingQueue<Runnable> blockingQueue;

    private final Integer queueSize;

    private final static Double DEFAULT_POP_RATIO = 0.2;

    private static final Integer DEFAULT_PAGE_SIZE = 50;

    public YyEdiShipAsyncJob(@Value("${yyedi.ship.task.queue.size:50}") int queueSize,
                             @Value("${yyedi.ship.task.pool.core.size:10}") int corePoolSize,
                             @Value("${yyedi.ship.task.pool.max.size:10}") int maxPoolSize) {
        this.queueSize = queueSize;
        blockingQueue = new ArrayBlockingQueue<>(queueSize);
        this.executorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.MINUTES,
                blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("yyedi-ship-handle-%d").build(),
                (r, executor) -> log.error("task {} is rejected", r));
    }

//    @Scheduled(cron = "0 */3 * * * ?")
    public void producer() {
        if (canProduce()) {
            super.producer();
        } else {
            log.error("Production is not allowed at present time");
        }
    }

//    @PostConstruct
    public void consumer() {
        log.info("[YYEDI SHIP CONSUMER START...]");
        super.consumer();
    }

    @Override
    protected String getKeySuffix() {
        return BizType.FIRST.val().toString();
    }

    @Override
    protected Map<String, List<Long>> getPushIds() {
        Map<String, List<Long>> map = Maps.newHashMap();
        Map<String, Object> param = Maps.newHashMap();
        param.put("status", 0);
        param.put("time", 3);
        param.put("type", 10);
        Integer pageNo = 1;
        Integer pageSize = size();
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        while (true) {
            Response<Paging<AutoCompensation>> response = autoCompensationReadService.pagination(pageNo, pageSize, param);
            if (!response.isSuccess()) {
                log.error("fail to find async task");
                break;
            }
            Paging<AutoCompensation> pagination = response.getResult();
            final List<AutoCompensation> autoCompensations = pagination.getData();
            if (CollectionUtils.isEmpty(autoCompensations)) {
                break;
            }
            List<Long> ids = autoCompensations.stream().map(AutoCompensation::getId).collect(Collectors.toList());
            map.put(DateTime.now().toString(), ids);
            if (!Objects.equals(autoCompensations.size(), pageSize)) {
                break;
            }
            pageNo++;
        }
        return map;
    }

    @Override
    protected void waitHandle(List<Long> ids) {
        autoCompensationWriteService.updateStatus(ids, HandleStatus.HANDLE_ING.status());
    }

    @Override
    protected void consume(List<Long> ids) {
        executorService.submit(new YyEdiShipTask(ids));
    }

    @Override
    protected Integer getPopSize() {
        int remainingCapacity = blockingQueue.remainingCapacity();
        if (remainingCapacity > queueSize * DEFAULT_POP_RATIO) {
            return remainingCapacity;
        }
        return 0;
    }

    public class YyEdiShipTask implements Runnable {

        private List<Long> ids;

        public YyEdiShipTask(List<Long> ids) {
            this.ids = ids;
        }

        @Override
        public void run() {
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }
            Response<List<AutoCompensation>> response1 = autoCompensationReadService.findByIdsAndStatus(ids, HandleStatus.HANDLE_ING.status());
            if (!response1.isSuccess()) {
                log.error("find async task failed,cause:{}", response1.getError());
                throw new JsonResponseException(response1.getError());
            }
            List<AutoCompensation> autoCompensations = response1.getResult();

            List<Long> successIds = Lists.newArrayList();
            autoCompensations.forEach(autoCompensation -> {
                try {
                    Map<String, String> extra = autoCompensation.getExtra();
                    if (Objects.nonNull(extra.get("param"))) {
                        Map<String, Long> param = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(extra.get("param"), JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(HashMap.class, String.class, Long.class));
                        Long shipmentId = Long.valueOf(param.get(TradeConstants.SHIPMENT_ID));
                        log.info("try to sync shipment(id:{}) to hk", shipmentId);
                        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
                        //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
                        hkShipmentDoneLogic.doneShipment(shipment);
                        //同步pos单到恒康
                        Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                        if (!response.isSuccess()) {
                            Map<String, Object> param1 = Maps.newHashMap();
                            param1.put("shipmentId", shipment.getId());
                            autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK, response.getError());
                        }
                        successIds.add(autoCompensation.getId());
                    }
                } catch (Exception e) {
                    log.error("yyedi ship async task(id:{}) failed,cause:{}", autoCompensation.getId(), Throwables.getStackTraceAsString(e));
                    Map<String, String> extra = Maps.newHashMap();
                    autoCompensation.setExtra(extra);
                    autoCompensation.getExtra().put("error", Throwables.getStackTraceAsString(e).substring(0, 512));
                    autoCompensateLogic.autoCompensationTaskExecuteFail(autoCompensation);
                }
            });
            //成功的状态置为已处理
            autoCompensationWriteService.updateStatus(successIds, HandleStatus.HANDLED.status());
        }
    }

    @PreDestroy
    public void shutdown(){
        this.executorService.shutdown();
    }

    /**
     * 任务状态
     */
    public enum HandleStatus {

        WAIT_HANDLE(0, "待处理"),
        HANDLE_ING(1, "处理中"),
        HANDLED(2, "已处理");

        private Integer status;

        private String desc;

        HandleStatus(Integer status, String desc) {
            this.status = status;
            this.desc = desc;
        }

        public Integer status() {
            return this.status;
        }

        public String desc() {
            return this.desc;
        }
    }

}
