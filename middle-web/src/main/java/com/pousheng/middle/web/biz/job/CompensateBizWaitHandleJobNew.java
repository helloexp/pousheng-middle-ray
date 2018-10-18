package com.pousheng.middle.web.biz.job;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.ConcurrentSkipBizException;
import com.pousheng.middle.web.order.job.AbstractAsyncJob;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 中台业务处理-处理从未处理过的任务的job
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@RestController
@Component
public class CompensateBizWaitHandleJobNew extends AbstractAsyncJob {

    @Autowired
    private CompensateBizProcessor compensateBizProcessor;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private final ExecutorService executorService;

    private final BlockingQueue<Runnable> blockingQueue;

    private final Integer queueSize;

    private final static Double DEFAULT_POP_RATIO = 0.2;

    private static final Integer DEFAULT_PAGE_SIZE = 20;

    public CompensateBizWaitHandleJobNew(@Value("${biz.task.queue.size:10000}") int queueSize,
                                         @Value("${biz.task.pool.core.size:4}")int corePoolSize,
                                         @Value("${biz.task.pool.max.size:8}")int maxPoolSize) {
        this.queueSize = queueSize;
        this.blockingQueue = new LinkedBlockingQueue<>(queueSize);
        this.executorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10L, TimeUnit.MINUTES,
                blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("biz-handle-%d").build(),
                (r, executor) -> log.error("task {} is rejected", r));
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void producer() {
        if (canProduce()) {
            super.producer();
        } else {
            log.error("Production is not allowed at present time");
        }
    }

    //@PostConstruct
    public void consumer() {
        log.info("[BIZ CONSUMER START...]");
        super.consumer();
    }


    //手动拉取
    @GetMapping(value = "/api/biz/consumer", produces = MediaType.APPLICATION_JSON_VALUE)
    public String consumerOrder() {
        log.info("[BIZ CONSUMER START...]");
        super.consumer();

        return "success";
    }

    @Override
    protected String getKeySuffix() {
        return BizType.SECOND.val().toString();
    }

    @Override
    protected Map<String, List<Long>> getPushIds() {
        Map<String, List<Long>> map = Maps.newHashMap();
        Integer pageNo = 1;
        Integer pageSize = size();
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        //先查状态为待处理的
        while (true) {
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                break;
            }
            List<PoushengCompensateBiz> poushengCompensateBizs = response.getResult().getData();
            map.put(DateTime.now().toString(), poushengCompensateBizs.stream().map(PoushengCompensateBiz::getId).collect(Collectors.toList()));
            if (poushengCompensateBizs.size() < pageSize) {
                break;
            }
            pageNo ++;
        }
         //再查状态为失败，但是重试次数小于3次的
        pageNo = 1;
        while (true) {
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setStatus(PoushengCompensateBizStatus.FAILED.name());
            criteria.setIgnoreCnt(3);
            Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                break;
            }
            List<PoushengCompensateBiz> poushengCompensateBizs = response.getResult().getData();
            map.put(DateTime.now().toString(), poushengCompensateBizs.stream().map(PoushengCompensateBiz::getId).collect(Collectors.toList()));
            if (poushengCompensateBizs.size() < pageSize) {
                break;
            }
            pageNo ++;
        }
        return map;
    }

    @Override
    protected void waitHandle(List<Long> ids) {
       Response<Boolean> response = poushengCompensateBizWriteService.batchUpdateStatus(ids, PoushengCompensateBizStatus.PROCESSING.name());
       if (response.isSuccess()) {
           if (log.isDebugEnabled()) {
               log.debug("batch update ids: {} status: PROCESSING SUCCESS", ids);
           }
       }
    }

    @Override
    protected void consume(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("pop ids:{} from queue", ids);
        }
        executorService.submit(new CompensateBizWaitHandleTask(ids));
    }

    @Override
    protected Integer getPopSize() {
        int remainingCapacity = blockingQueue.remainingCapacity();
        if (remainingCapacity > queueSize * DEFAULT_POP_RATIO) {
            return remainingCapacity;
        }
        return 0;
    }

    @PreDestroy
    public void shutdown(){
        this.executorService.shutdown();
    }

    public class CompensateBizWaitHandleTask implements Runnable {

        private List<Long> ids;

        public CompensateBizWaitHandleTask(List<Long> ids) {
            this.ids = ids;
        }

        @Override
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("The task(ids:{}) is being handle", ids);
            }
            Response<List<PoushengCompensateBiz>> response = poushengCompensateBizReadService.findByIdsAndStatus(ids, PoushengCompensateBizStatus.PROCESSING.name());
            if (!response.isSuccess()) {
                log.error("fail to find compensate biz by ids:{},cause:{}", ids, response.getError());
                return;
            }
            List<PoushengCompensateBiz> poushengCompensateBizs = response.getResult();
            List<Long> successIds = Lists.newArrayList();
            poushengCompensateBizs.forEach(poushengCompensateBiz -> {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("biz task id:{} process", poushengCompensateBiz.getId());
                    }
                    compensateBizProcessor.doProcess(poushengCompensateBiz);
                    successIds.add(poushengCompensateBiz.getId());
                } catch (ConcurrentSkipBizException cse) {
                    log.warn("biz task {} is executing.skip it.", poushengCompensateBiz.getId());
                } catch (BizException e0) {
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType(), e0);
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                    poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(), e0.getMessage(), (poushengCompensateBiz.getCnt() + 1));
                } catch (Exception e1) {
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType(), e1);
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                    poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(), e1.getMessage(), (poushengCompensateBiz.getCnt() + 1));
                }
            });
            if (!CollectionUtils.isEmpty(successIds)) {
                poushengCompensateBizWriteService.batchUpdateStatus(successIds, PoushengCompensateBizStatus.SUCCESS.name());
            }
            if (log.isDebugEnabled()) {
                log.debug("The task(ids:{}) has been handled, successIds: {}", ids, successIds);
            }
        }
    }
}
