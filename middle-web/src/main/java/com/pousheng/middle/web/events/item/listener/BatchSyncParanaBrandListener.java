package com.pousheng.middle.web.events.item.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.events.item.BatchSyncParanaBrandEvent;
import com.pousheng.middle.web.events.item.DumpSyncParanaBrandEvent;
import com.pousheng.middle.web.task.SyncErrorData;
import com.pousheng.middle.web.task.SyncParanaTaskRedisHandler;
import com.pousheng.middle.web.task.SyncTask;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
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
import org.springframework.util.CollectionUtils;

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
    static final Integer BATCH_SIZE = 100;     // 批处理数量



    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onBatchSyncBrand(BatchSyncParanaBrandEvent event){

        log.info("batch sync brand to parana start");
        String taskId = event.getTaskId();

        List<Brand> brands = findByIds(event.getBrandIds());
        List<SyncErrorData> errorDatas = Lists.newArrayListWithCapacity(brands.size());

        Response<Boolean> syncRes = sync(brands);
        if(!syncRes.isSuccess()){
            log.error("sync  brand to parana fail,error:{}",syncRes.getError());
            SyncErrorData errorData = new SyncErrorData();
            errorData.setError(syncRes.getError());
            errorDatas.add(errorData);
        }

        handResult(errorDatas,taskId);
        log.info("batch sync brand to parana end");

    }





    @Subscribe
    public void onDumpSyncBrand(DumpSyncParanaBrandEvent event){

        log.info("dump sync brand to parana start");
        String taskId = event.getTaskId();

        List<SyncErrorData> syncErrorDatas = handleAll();

        handResult(syncErrorDatas,taskId);


        log.info("dump sync brand to parana end");

    }


    private void handResult(List<SyncErrorData> errorDatas,String taskId){


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
    }


    private Response<Boolean> sync(List<Brand> brands){
        List<OpenClientBrand> openClientBrands = Lists.newArrayListWithCapacity(brands.size());
        for (Brand brand: brands ){
            OpenClientBrand openClientBrand = new OpenClientBrand();
            BeanMapper.copy(brand,openClientBrand);
            openClientBrands.add(openClientBrand);
        }
        return syncParanaBrandService.syncBrands(openClientBrands);
        //return Response.ok(Boolean.TRUE);
    }



    private List<Brand> findByIds(List<Long> ids){
        Response<List<Brand>> childRes = brandReadService.findByIds(ids);
        if(!childRes.isSuccess()){
            log.error("find brand by ids:{} fail,error:{}",ids,childRes.getError());
            return Lists.newArrayList();
        }
        return childRes.getResult();
    }


    private List<SyncErrorData> handleAll(){

        List<SyncErrorData> errorDatas = Lists.newArrayList();
        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,errorDatas);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,errorDatas);
        }

        return errorDatas;
    }






    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,List<SyncErrorData> syncErrorDatas) {
        Response<Paging<Brand>> pagingRes = brandReadService.pagination(pageNo, size, null);
        if(!pagingRes.isSuccess()){
            log.error("paging brand fail error:{}",pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<Brand> paging = pagingRes.getResult();
        List<Brand> brands = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(brands)) {
            return Boolean.FALSE;
        }

        for (Brand brand : brands){
            Response<Boolean> syncRes = sync(Lists.newArrayList(brand));
            if(!syncRes.isSuccess()){
                log.error("sync brand (id:{}) to parana fail,error:{}",brand.getId(),syncRes.getError());
                SyncErrorData errorData = new SyncErrorData();
                errorData.setId(brand.getId());
                errorData.setName(brand.getName());
                errorData.setError(syncRes.getError());
                syncErrorDatas.add(errorData);
            }

        }

        int current = brands.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }



}
