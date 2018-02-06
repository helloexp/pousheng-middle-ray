package com.pousheng.middle.web.events.item.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.open.api.dto.ErpStock;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.item.BatchSyncStockEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class BatchSyncStockListener {


    @Autowired
    private EventBus eventBus;
    @Autowired
    private WarehouseCacher warehouseCacher;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @Autowired
    private StockPusher stockPusher;

    private static final TypeReference<List<ErpStock>> LIST_OF_ERP_STOCK = new TypeReference<List<ErpStock>>() {};

    private static final DateTimeFormatter dft = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");



    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onBatchSyncStockEvent(BatchSyncStockEvent event){

        Integer total = event.getTotal();
        String data = event.getData();
        log.debug("batch sync stock to middle start ,total:{}",total);

        try {
            List<ErpStock> erpStocks = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(data, LIST_OF_ERP_STOCK);
            List<StockDto> stockDtos = make(erpStocks);
            Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
            if(!r.isSuccess()){
                log.error("failed to sync {} stocks, data:{}, error code:{}", total, data, r.getError());
                throw new OPServerException(200,r.getError());
            }
            //触发库存推送
            List<String> skuCodes = Lists.newArrayList();
            for (StockDto stockDto : stockDtos) {
                skuCodes.add(stockDto.getSkuCode());
            }
            stockPusher.submit(skuCodes);
        } catch (Exception e) {
            log.error("failed to sync {} stocks, data:{}, cause:{}", total, data, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"stock.data.invalid");
        }

        log.debug("batch sync stock to middle end");

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
            } catch (Exception e) {
                log.error("failed to sync {}, cause:{}", erpStock, Throwables.getStackTraceAsString(e));
                throw new ServiceException("make.middle.data.fail");
            }
        }
        return result;
    }






}
