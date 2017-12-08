package com.pousheng.middle.web.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@RestController
@RequestMapping("/api/warehouse")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE)
public class Warehouses {

    @RpcConsumer
    private WarehouseWriteService warehouseWriteService;

    @RpcConsumer
    private WarehouseReadService warehouseReadService;
    @Autowired
    private ErpClient erpClient;

    @RpcConsumer
    private MiddleStockPushLogReadSerive middleStockPushLogReadSerive;

    @RpcConsumer
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("新建")
    public Long create(@RequestBody Warehouse warehouse) {
        Response<Long> r = warehouseWriteService.create(warehouse);
        if (!r.isSuccess()) {
            log.error("failed to batchCreate {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
    @RequestMapping(value = "/trigger/push",method = RequestMethod.GET)
    public Boolean triggerPush(@RequestParam(value = "id") Long id){
        log.info("[trigger push ] id={}",id);
        Response<Warehouse> warehouseResponse=warehouseReadService.findById(id);
        if (!warehouseResponse.isSuccess()){
            log.error("find Warehouse failed cause={}",warehouseResponse.getError());
            throw new JsonResponseException(warehouseResponse.getError());
        }
        Warehouse warehouse=warehouseResponse.getResult();
        Map<String,String> extra=warehouse.getExtra();
        if (extra==null||!extra.containsKey("isNew")|| !Objects.equal(extra.get("isNew"),"true")){
            throw new JsonResponseException("warehouse.status.is.not.allowed.trigger.push");
        }
        Map<String,String> map=Maps.newHashMap();
        map.put("stock",warehouse.getInnerCode());
        erpClient.get("/common/erp/inv/getinstockcount",map);
        return Boolean.TRUE;

    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("更新")
    public Boolean update(@RequestBody Warehouse warehouse) {
        Response<Boolean> r = warehouseWriteService.update(warehouse);
        if (!r.isSuccess()) {
            log.error("failed to update {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean delete(@PathVariable("id") Long id) {
        Response<Boolean> r = warehouseWriteService.deleteById(id);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }

        return true;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Warehouse> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                        @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                        @RequestParam(required = false, value = "code") String code,
                                        @RequestParam(required = false, value="codePrefix") String codePrefix,
                                        @RequestParam(required = false, value = "name") String namePrefix,
                                        @RequestParam(required = false, value="outCode") String outCode) {
        Map<String, Object> params = Maps.newHashMap();
        if (StringUtils.hasText(code)) {
            //params.put("code", code);
            if(Character.isDigit(code.charAt(0))){
                params.put("code", code.substring(0,3)+"-"+code);
            }else{
                params.put("code", code.substring(0,4)+"-"+code);
            }
        }
        if (StringUtils.hasText(codePrefix)) {
            params.put("codePrefix", codePrefix);
        }

        if (StringUtils.hasText(namePrefix)) {
            params.put("name", namePrefix);
        }

        if(StringUtils.hasText(outCode)){
            params.put("outCode", outCode);
        }
        Response<Paging<Warehouse>> r = warehouseReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination warehouse with params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();

    }

    @RequestMapping(value = "/create/push/log",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public void createStockPushLog(@RequestBody StockPushLog stockPushLog){
        Response<Long> response = middleStockPushLogWriteService.create(stockPushLog);
        if (!response.isSuccess()){
            log.error("fffff");
        }
    }

    /**
     * 根据主键查询推送日志
     * @param id 表的主键
     * @return
     */
    @RequestMapping(value = "/stock/push/log/by/id",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<StockPushLog> queryStockPushLogById(@RequestParam("id") Long id){
        Response<StockPushLog> r = middleStockPushLogReadSerive.findById(id);
        if (!r.isSuccess()){
                log.error("failed to query stockPushLog with is:{}, error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r;
    }

    /**
     * 分页查询库存推送日志
     * @param pageNo 页码
     * @param pageSize 每页记录数
     * @param skuCode  条码
     * @param shopId   店铺id
     * @param shopName 店铺名称
     * @param status   推送状态
     * @return
     */
    @RequestMapping(value = "/stock/push/log/paging",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<StockPushLog>> paginationStockPushLog(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                   @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                   @RequestParam(required = false,value = "skuCode") String skuCode,
                                   @RequestParam(required = false,value = "shopId")Long shopId,
                                   @RequestParam(required = false,value = "shopName")String shopName,@RequestParam(required = false) Integer status){
        Map<String, Object> params = Maps.newHashMap();
        if (StringUtils.hasText(skuCode)){
            params.put("skuCode",skuCode);
        }
        if(shopId!=null){
            params.put("shopId",shopId);
        }
        if (StringUtils.hasText(shopName)){
            params.put("shopName",shopName);
        }
        if(status!=null){
            params.put("status",status);
        }
        Response<Paging<StockPushLog>>  r = middleStockPushLogReadSerive.pagination(pageNo,pageSize,params);
        if(!r.isSuccess()){
            log.error("failed to pagination stockPushLog with params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r;
    }

}
