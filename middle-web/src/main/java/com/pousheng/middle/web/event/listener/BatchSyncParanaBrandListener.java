package com.pousheng.middle.web.event.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.event.BatchSyncParanaBrandEvent;
import com.pousheng.middle.web.task.SyncErrorData;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.parana.dto.OpenClientBrand;
import io.terminus.open.client.parana.item.SyncParanaBrandService;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class BatchSyncParanaBrandListener {


    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private BrandReadService brandReadService;
    @Autowired
    private SyncParanaBrandService syncParanaBrandService;
    @Autowired
    private SyncParanaTaskRedisHandler syncParanaTaskRedisHandler;

    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onSyncCategory(BatchSyncParanaBrandEvent event){

        log.info("batch sync brand to parana start");
        String taskId = event.getTaskId();

        List<Brand> brands = findByIds(event.getBrandIds());
        List<SyncErrorData> errorDatas = Lists.newArrayListWithCapacity(brands.size());

        Response<Boolean> syncRes = sync(brands);
        if(!syncRes.isSuccess()){
            log.error("sync  brand:{} to parana fail,error:{}",brands,syncRes.getError());
            SyncErrorData errorData = new SyncErrorData();
            errorData.setError(syncRes.getError());
            errorDatas.add(errorData);
        }


        if (!Arguments.isNullOrEmpty(errorDatas)){
            log.error("sync brand failed data = {}", errorDatas);
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(-1);
            task.setError(JsonMapper.nonDefaultMapper().toJson(errorDatas));
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }else {
            log.info("sync brand to  parana success......");
            //更新redis task状态
            SyncTask task = new SyncTask();
            task.setStatus(2);
            syncParanaTaskRedisHandler.updateTask(taskId,task);

        }

        log.info("batch sync brand to parana end");

    }

    private Response<Boolean> sync(List<Brand> brands){
        List<OpenClientBrand> openClientBrands = Lists.newArrayListWithCapacity(brands.size());
        for (Brand brand: brands ){
            OpenClientBrand openClientBrand = new OpenClientBrand();
            BeanMapper.copy(brand,openClientBrand);
            openClientBrands.add(openClientBrand);
        }
        //return syncParanaBrandService.syncBrands(openClientBrands);
        return Response.ok(Boolean.TRUE);
    }



    private List<Brand> findByIds(List<Long> ids){
        Response<List<Brand>> childRes = brandReadService.findByIds(ids);
        if(!childRes.isSuccess()){
            log.error("find brand by ids:{} fail,error:{}",ids,childRes.getError());
            return Lists.newArrayList();
        }
        return childRes.getResult();
    }
}
