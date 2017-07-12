package com.pousheng.middle.open.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.api.dto.ErpStock;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final TypeReference<List<ErpStock>> LIST_OF_ERP_STOCK = new TypeReference<List<ErpStock>>() {
    };

    @Autowired
    private WarehouseCacher warehouseCacher;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @OpenMethod(key = "hk.stock.api", paramNames = {"total", "data"}, httpMethods = RequestMethod.POST)
    public void onStockChanged(@RequestParam("total")Integer total, @RequestParam("data")String data){
        log.info("ERPSTOCK -- begin to handle erp stock:{}", data);
        try {
            List<ErpStock> erpStocks = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(data, LIST_OF_ERP_STOCK);
            List<StockDto> stockDtos = make(erpStocks);
            Response<Boolean> r = warehouseSkuWriteService.syncStock(stockDtos);
            if(!r.isSuccess()){
                log.error("failed to sync {} stocks, data:{}, error code:{}", total, data, r.getError());
            }
        } catch (Exception e) {
            log.error("failed to sync {} stocks, data:{}, cause:{}", total, data, Throwables.getStackTraceAsString(e));
        }

    }

    private List<StockDto> make(List<ErpStock> erpStocks) {
        List<StockDto> result = Lists.newArrayListWithCapacity(erpStocks.size());
        for (ErpStock erpStock : erpStocks) {
            StockDto stockDto = new StockDto();
            stockDto.setSkuCode(erpStock.getBarcode());
            stockDto.setQuantity(erpStock.getQuantity());
            stockDto.setUpdatedAt(erpStock.getModify_time());

            String warehouseCode = erpStock.getCompany_id()+"-"+erpStock.getStock_id();
            Warehouse warehouse = warehouseCacher.findByCode(warehouseCode);
            stockDto.setWarehouseId(warehouse.getId());
            result.add(stockDto);
        }
        return result;
    }
}
