package com.pousheng.middle.open.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.enums.StockTaskType;
import com.pousheng.middle.open.api.dto.ErpStock;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.SkuStockTask;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.SkuStockTaskWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 恒康主动推sku的库存过来
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-10
 */
@OpenBean
@Slf4j
public class WarehouseStockApi {


    @Autowired
    private EventBus eventBus;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private SkuStockTaskWriteService skuStockTaskWriteService;

    private static final TypeReference<List<ErpStock>> LIST_OF_ERP_STOCK = new TypeReference<List<ErpStock>>() {};

    private static final DateTimeFormatter dft = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    @Getter
    @Setter
    @Value("${stock.sync.task.full.time.start:02:00:00}")
    String stockSyncTaskFullTimeStart;
    @Getter
    @Setter
    @Value("${stock.sync.task.full.time.end:02:30:00}")
    String stockSyncTaskFullTimeEnd;


    @OpenMethod(key = "hk.stock.api", paramNames = {"total", "data"}, httpMethods = RequestMethod.POST)
    public void onStockChanged(@RequestParam("total")Integer total, @RequestParam("data")String data){
        if(log.isDebugEnabled()){
            log.debug("HK-STOCK-API-START param: total [{}] data [{}]", total,data);
        }
        List<ErpStock> erpStocks;
        try {
           erpStocks = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(data, LIST_OF_ERP_STOCK);
        } catch (Exception e) {
            log.error("failed to sync {} stocks, data:{}, cause:{}", total, data, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"stock.data.invalid");
        }
        List<StockDto> stockDtos = make(erpStocks);
        SkuStockTask task = new SkuStockTask();
        task.setSkuCount(total);
        task.setStockDtoList(stockDtos);
        task.setStatus(0);
        //任务表类型，如果是2：00至2：30则认为是全量
        if(this.isFullRunTime()){
            task.setType(StockTaskType.FULL_TYPE.getValue());
        }else{
            task.setType(StockTaskType.INCR_TYPE.getValue());
        }

        Response<Long> response = skuStockTaskWriteService.create(task);
        if (!response.isSuccess()) {
            log.error("create task:{} fail,error:{}",task,response.getError());
            throw new OPServerException(200,response.getError());
        }

        /*BatchSyncStockEvent syncStockEvent = new BatchSyncStockEvent();
        syncStockEvent.setStockDtos(stockDtos);
        eventBus.post(syncStockEvent);*/
        if(log.isDebugEnabled()){
            log.debug("HK-STOCK-API-END param: total [{}] data [{}]", total,data);
        }

    }



    private List<StockDto> make(List<ErpStock> erpStocks) {
        List<StockDto> result = Lists.newArrayListWithCapacity(erpStocks.size());
        for (ErpStock erpStock : erpStocks) {
            try {
                StockDto stockDto = new StockDto();
                stockDto.setSkuCode(erpStock.getBarcode());
                stockDto.setQuantity(erpStock.getQuantity());
                stockDto.setUpdatedAt(dft.parseDateTime(erpStock.getModify_time()).toDate());

                String warehouseCode = erpStock.getCompany_id()+"-"+erpStock.getStock_id();
                Warehouse warehouse = warehouseCacher.findByCode(warehouseCode);
                stockDto.setWarehouseId(warehouse.getId());
                result.add(stockDto);
            } catch (ServiceException e) {
                log.error("failed to sync stock{}, cause:{}", erpStock, Throwables.getStackTraceAsString(e));
                throw new OPServerException(200,e.getMessage());
            }
        }
        return result;
    }


    @OpenMethod(key = "erp.stock.api", paramNames = {"total", "data"}, httpMethods = RequestMethod.POST)
    public void onSyncErpStockChanged(@RequestParam("total")Integer total, @RequestParam("data")String data){
        if(log.isDebugEnabled()){
            log.debug("ERP-STOCK-API-START param: total [{}] data [{}]", total,data);
        }
        this.onStockChanged(total,data);
        if(log.isDebugEnabled()){
            log.debug("ERP-STOCK-API-END param: total [{}] data [{}]", total,data);
        }

    }


    /**
     * @Description 是否在全量写入TASK的时间段内
     * @Date        2018/5/29
     * @param
     * @return
     */
    private boolean isFullRunTime(){
        java.time.LocalTime localTime = java.time.LocalTime.now();
        java.time.LocalTime startTime = java.time.LocalTime.parse(stockSyncTaskFullTimeStart);
        java.time.LocalTime endTime = java.time.LocalTime.parse(stockSyncTaskFullTimeEnd);
        if(localTime.compareTo(startTime)>0 && localTime.compareTo(endTime)<0){
            return true;
        }
        return false;
    }



}
