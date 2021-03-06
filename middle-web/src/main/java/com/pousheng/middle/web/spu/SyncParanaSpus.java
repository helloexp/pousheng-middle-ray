package com.pousheng.middle.web.spu;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.web.events.item.BatchSyncParanaSpuEvent;
import com.pousheng.middle.web.events.item.DumpSyncParanaSpuEvent;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.parana.item.SyncParanaSpuService;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@RestController
@Slf4j
@RequestMapping("/api/spu")
public class SyncParanaSpus {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @RpcConsumer
    private SpuReadService spuReadService;
    @Autowired
    private SyncParanaSpuService syncParanaSpuService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;
    @Autowired
    private EventBus eventBus;

    @ApiOperation("根据id同步")
    @LogMe(description = "根据id同步u", ignore = true)
    @RequestMapping(value = "/{id}/sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> syncSpu(@PathVariable(name = "id") @LogMeContext Long spuId){

        if(log.isDebugEnabled()){
            log.debug("API-SPU-SYNC-START param: spuId [{}]",spuId);
        }
        Response<FullSpu> fullSpuRes = spuReadService.findFullInfoBySpuId(spuId);
        if(!fullSpuRes.isSuccess()){
            log.error("find full spu by spu id:{} fail,error:{}",spuId,fullSpuRes.getError());
            throw new JsonResponseException(fullSpuRes.getError());
        }
        Response<Boolean> resp = syncParanaSpuService.syncSpus(mapper.toJson(fullSpuRes.getResult()));
        if(log.isDebugEnabled()){
            log.debug("API-SPU-SYNC-END param: spuId [{}] ,resp: [{}]",spuId,resp.getResult());
        }
        return resp;
    }

    /**
     * 批量同步spu
     * @return 任务ID
     */
    @ApiOperation("批量同步spu")
    @LogMe(description = "批量同步spu", ignore = true)
    @RequestMapping(value = "/batch-sync/{ids}", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String batchSynSpu(@PathVariable(name = "ids") @LogMeContext String spuIds){

        if(log.isDebugEnabled()){
            log.debug("API-SPU-BATCHSYNSPU-START param: spuIds [{}]",spuIds);
        }
        if(Strings.isNullOrEmpty(spuIds)){
            log.error("batch sync spu fail,because ids is null");
            throw new JsonResponseException("param.is.invalid");
        }

        List<Long> ids = Splitters.splitToLong(spuIds,Splitters.COMMA);
        SyncTask task = new SyncTask();
        task.setStatus(1);
        String taskId = syncParanaTaskRedisHandler.saveTask(task);
        BatchSyncParanaSpuEvent event = new BatchSyncParanaSpuEvent();
        event.setTaskId(taskId);
        event.setSpuIds(ids);
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-SPU-BATCHSYNSPU-END param: spuIds [{}] ,resp: [{}]",spuIds,taskId);
        }
        return taskId;
    }

    /**
     * 全量同步spu
     * @return 任务ID
     */
    @ApiOperation("全量同步spu")
    @LogMe(description = "全量同步spu", ignore = true)
    @RequestMapping(value = "/dump-sync", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String batchSynCategory(){
        if(log.isDebugEnabled()){
            log.debug("API-SPU-DUMP-SYNC-START noparam: ");
        }
        SyncTask task = new SyncTask();
        task.setStatus(1);
        String taskId = syncParanaTaskRedisHandler.saveTask(task);
        DumpSyncParanaSpuEvent event = new DumpSyncParanaSpuEvent();
        event.setTaskId(taskId);
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-SPU-DUMP-SYNC-END noparam: ,resp: [{}]",taskId);
        }
        return taskId;

    }
}
