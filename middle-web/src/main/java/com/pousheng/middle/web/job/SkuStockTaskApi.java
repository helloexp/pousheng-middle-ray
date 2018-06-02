package com.pousheng.middle.web.job;

import com.pousheng.middle.open.api.WarehouseStockApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/6/2
 */
@Api(description = "库存同步全量同步时间设置API")
@RestController
@Slf4j
@RequestMapping("/api/job/stock")
public class SkuStockTaskApi {

    @Autowired
    WarehouseStockApi warehouseStockApi;
    @Autowired
    SkuStockThirdTaskTimeIndexer skuStockThirdTaskTimeIndexer;
    @Autowired
    SkuStockTaskTimeIndexer skuStockTaskTimeIndexer;

    @ApiOperation("获取当前库存全量同步时间设置")
    @RequestMapping(value = "/sync/full/time", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map getStockSyncTaskFullTime() {
        Map map = new TreeMap();
        map.put("stockSyncTaskFullTimeStart",warehouseStockApi.getStockSyncTaskFullTimeStart());
        map.put("stockSyncTaskFsullTimeEnd",warehouseStockApi.getStockSyncTaskFullTimeEnd());
        map.put("stockSyncMiddleFullTimeStart",skuStockTaskTimeIndexer.getStockSyncMiddleFullTimeStart());
        map.put("stockSyncMiddleFullTimeEnd",skuStockTaskTimeIndexer.getStockSyncMiddleFullTimeEnd());
        map.put("stockSyncThirdFullTimeStart",skuStockThirdTaskTimeIndexer.getStockSyncThirdFullTimeStart());
        map.put("stockSyncThirdFullTimeEnd",skuStockThirdTaskTimeIndexer.getStockSyncThirdFullTimeEnd());
        return map;
    }

    @ApiOperation("设置库存全量同步-宝胜写入任务表时间段")
    @RequestMapping(value = "/sync/task/time", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean setStockSyncTaskFullTime(@RequestParam(required = true)String stockSyncTaskFullTimeStart,@RequestParam(required = true)String stockSyncTaskFsullTimeEnd){
        log.info("Setting StockSyncTaskFullTime:{},stockSyncTaskFsullTimeEnd:{}",stockSyncTaskFullTimeStart,stockSyncTaskFsullTimeEnd);
        warehouseStockApi.setStockSyncTaskFullTimeStart(stockSyncTaskFullTimeStart);
        warehouseStockApi.setStockSyncTaskFullTimeEnd(stockSyncTaskFsullTimeEnd);
        return true;
    }

    @ApiOperation("设置库存全量同步-同步中台时间段")
    @RequestMapping(value = "/sync/middle/time", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean setStockSyncMiddleFullTime(@RequestParam(required = true)String stockSyncMiddleFullTimeStart, @RequestParam(required = true)String stockSyncMiddleFullTimeEnd){
        skuStockTaskTimeIndexer.setStockSyncMiddleFullTimeStart(stockSyncMiddleFullTimeStart);
        skuStockTaskTimeIndexer.setStockSyncMiddleFullTimeEnd(stockSyncMiddleFullTimeEnd);
        return true;
    }

    @ApiOperation("设置库存全量同步-同步第三方时间段")
    @RequestMapping(value = "/sync/third/time", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean setStockSyncThirdFullTime(@RequestParam(required = true)String stockSyncThirdFullTimeStart,@RequestParam(required = true)String stockSyncThirdFullTimeEnd){
        skuStockThirdTaskTimeIndexer.setStockSyncThirdFullTimeStart(stockSyncThirdFullTimeStart);
        skuStockThirdTaskTimeIndexer.setStockSyncThirdFullTimeEnd(stockSyncThirdFullTimeEnd);
        return true;
    }



}
