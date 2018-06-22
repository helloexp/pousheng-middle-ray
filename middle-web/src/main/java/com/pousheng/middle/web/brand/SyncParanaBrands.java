package com.pousheng.middle.web.brand;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.web.events.item.BatchSyncParanaBrandEvent;
import com.pousheng.middle.web.events.item.DumpSyncParanaBrandEvent;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.parana.dto.OpenClientBrand;
import io.terminus.open.client.parana.item.SyncParanaBrandService;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
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
@RequestMapping("/api/brand")
public class SyncParanaBrands {


    @RpcConsumer
    private BrandReadService brandReadService;
    @Autowired
    private SyncParanaBrandService syncParanaBrandService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;
    @Autowired
    private EventBus eventBus;


    @RequestMapping(value = "/{id}/sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> synBrand(@PathVariable(name = "id") Long brandId){
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-ID-SYNC-START param: brandId [{}] ",brandId);
        }
        Response<Brand> brandRes = brandReadService.findById(brandId);
        if(!brandRes.isSuccess()){
            log.error("find brand by id:{} fail,error:{}",brandId,brandRes.getError());
            throw new JsonResponseException(brandRes.getError());
        }
        List<OpenClientBrand> openClientBrands = Lists.newArrayList();
        OpenClientBrand openClientBrand = new OpenClientBrand();
        Brand brand = brandRes.getResult();
        BeanMapper.copy(brand,openClientBrand);
        openClientBrands.add(openClientBrand);
        Response<Boolean> resp= syncParanaBrandService.syncBrands(openClientBrands);
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-ID-SYNC-END param: brandId [{}] ,resp: [{}]",brandId,resp);
        }
        return resp;
    }

    /**
     * 批量同步品牌
     * @return 任务ID
     */
    @RequestMapping(value = "/batch-sync/{ids}", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String batchSynBrand(@PathVariable(name = "ids") String brandIds){
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-BATCH-SYNC-IDS-START param: brandIds [{}] ",brandIds);
        }
        if(Strings.isNullOrEmpty(brandIds)){
            log.error("batch sync brand fail,because ids is null");
            throw new JsonResponseException("param.is.invalid");
        }

        List<Long> ids = Splitters.splitToLong(brandIds,Splitters.COMMA);
        SyncTask task = new SyncTask();
        task.setStatus(1);
        String taskId = syncParanaTaskRedisHandler.saveTask(task);
        BatchSyncParanaBrandEvent event = new BatchSyncParanaBrandEvent();
        event.setTaskId(taskId);
        event.setBrandIds(ids);
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-BATCH-SYNC-IDS-END param: brandIds [{}] ,resp: [{}]",brandIds,taskId);
        }
        return taskId;

    }

    /**
     * 全量同步品牌
     * @return 任务ID
     */
    @RequestMapping(value = "/dump-sync", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String batchSynCategory(){
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-DUMP-SYNC-START noparam: ");
        }
        SyncTask task = new SyncTask();
        task.setStatus(1);
        String taskId = syncParanaTaskRedisHandler.saveTask(task);
        DumpSyncParanaBrandEvent event = new DumpSyncParanaBrandEvent();
        event.setTaskId(taskId);
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-BRAND-DUMP-SYNC-END noparam: ,resp: [{}]",taskId);
        }
        return taskId;

    }
}
