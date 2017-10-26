package com.pousheng.middle.web.events.item.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.events.item.BatchSyncParanaSpuEvent;
import com.pousheng.middle.web.events.item.DumpSyncParanaSpuEvent;
import com.pousheng.middle.web.task.SyncErrorData;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.parana.item.SyncParanaSpuService;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class BatchSyncParanaSpuListener {


    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private SpuReadService spuReadService;
    @Autowired
    private SyncParanaSpuService syncParanaSpuService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;
    static final Integer BATCH_SIZE = 100;     // 批处理数量



    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onSyncSpu(BatchSyncParanaSpuEvent event){

        log.debug("batch sync spu to parana start");
        String taskId = event.getTaskId();
        List<Long> spuIds = event.getSpuIds();

        List<SyncErrorData> errorDatas = Lists.newArrayListWithCapacity(spuIds.size());

        batchSync(spuIds,errorDatas);
        handleResult(taskId,errorDatas);

        log.debug("batch sync spu to parana end");

    }


    @Subscribe
    public void onDumpSyncSpu(DumpSyncParanaSpuEvent event){

        log.info("dump sync spu to parana start");
        String taskId = event.getTaskId();

        List<SyncErrorData> errorDatas = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,errorDatas);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,errorDatas);
        }

        handleResult(taskId,errorDatas);

        log.info("dump sync spu to parana end");

    }

    private void  batchSync(List<Long> spuIds,List<SyncErrorData> errorDatas){
        List<FullSpu> fullSpus = findByIds(spuIds,errorDatas);

        for (FullSpu fullSpu : fullSpus){

            Response<Boolean> syncRes = sync(fullSpu);
            if(!syncRes.isSuccess()){
                log.error("sync spu:(id:{}) to parana fail,error:{}",fullSpu.getSpu().getId(),syncRes.getError());
                SyncErrorData errorData = new SyncErrorData();
                errorData.setId(fullSpu.getSpu().getId());
                errorData.setName(fullSpu.getSpu().getName());
                errorData.setError(syncRes.getError());
                errorDatas.add(errorData);
            }
        }

    }



    private void handleResult(String taskId,List<SyncErrorData> errorDatas){

        if (!Arguments.isNullOrEmpty(errorDatas)){
            log.error("sync spu failed data = {}", errorDatas);
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(-1);
            task.setError(JsonMapper.nonDefaultMapper().toJson(errorDatas));
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }else {
            log.info("sync spu to  parana success......");
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(2);
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }
    }

    private Response<Boolean> sync(FullSpu fullSpu){

        return syncParanaSpuService.syncSpus(mapper.toJson(fullSpu));
    }


    private List<FullSpu> findByIds(List<Long> ids,List<SyncErrorData> errorDatas){
        List<FullSpu> fullSpus = Lists.newArrayListWithCapacity(ids.size());
        for (Long spuId : ids){
            Response<FullSpu> fullSpuRes = spuReadService.findFullInfoBySpuId(spuId);
            if(!fullSpuRes.isSuccess()){
                log.error("find full spu by spu id:{} fail,error:{}",spuId,fullSpuRes.getError());
                SyncErrorData syncErrorData = new SyncErrorData();
                syncErrorData.setId(spuId);
                syncErrorData.setError(fullSpuRes.getError());
                errorDatas.add(syncErrorData);
            }

            fullSpus.add(fullSpuRes.getResult());
        }
        return fullSpus;
    }



    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,List<SyncErrorData> syncErrorDatas) {
        Response<Paging<Spu>> pagingRes = spuReadService.findByCategoryId(null,null,pageNo, size);
        if(!pagingRes.isSuccess()){
            log.error("paging brand fail error:{}",pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<Spu> paging = pagingRes.getResult();
        List<Spu> spus = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(spus)) {
            return Boolean.FALSE;
        }
        List<Long> spuIds = Lists.transform(spus, new Function<Spu, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Spu spu) {
                return spu.getId();
            }
        });

        batchSync(spuIds,syncErrorDatas);

        int current = spuIds.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }
}
