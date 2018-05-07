package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.erp.component.HkClient;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseServerInfo;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @Autowired
    private HkClient hkClient;

    @RpcConsumer
    private MiddleStockPushLogReadSerive middleStockPushLogReadSerive;

    @RpcConsumer
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;

    @Autowired
    private MposWarehousePusher warehousePusher;
    @Autowired
    private WarehouseCacher warehouseCacher;

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
        Map<String,String> map=Maps.newHashMap();
        map.put("stock",warehouse.getInnerCode());
        hkClient.get("common/erp/inv/getinstockcount",map);
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
                                        @RequestParam(required = false, value="outCode") String outCode,
                                        @RequestParam(required = false, value = "isMpos") Integer isMpos,
                                        @RequestParam(required = false, value = "companyId") String companyId) {
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
        if(isMpos != null){
            params.put("isMpos",isMpos);
        }
        if(StringUtils.hasText(companyId)){
            params.put("companyId",companyId);
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


    /**
     * @param id 库存id
     */
    @ApiOperation("库存mpos打标")
    @RequestMapping(value = "/{id}/make/flag",method = RequestMethod.PUT)
    public void makeMposFlag(@PathVariable Long id){
        Response<Warehouse> response = warehouseReadService.findById(id);
        if(!response.isSuccess()){
            log.error("fail to find warehouse by id:{},error:{}",id,response.getError());
            throw new JsonResponseException(response.getError());
        }
        Warehouse exist = response.getResult();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(exist.getId());
        warehouse.setIsMpos(1);
        Response<Boolean> res = warehouseWriteService.update(warehouse);
        if(!res.isSuccess()){
            log.error("update warehouse failed,error:{}",res.getError());
            throw new JsonResponseException(res.getError());
        }
        warehousePusher.addWarehouses(exist.getCompanyId(),exist.getExtra().get("outCode"));

        //刷新缓存
        warehouseCacher.refreshById(exist.getId());
    }

    /**
     * @param id 库存id
     */
    @ApiOperation("库存mpos取消打标")
    @RequestMapping(value = "/{id}/cancel/flag",method = RequestMethod.PUT)
    public void cancelMposFlag(@PathVariable Long id){
        Response<Warehouse> response = warehouseReadService.findById(id);
        if(!response.isSuccess()){
            log.error("fail to find warehouse by id:{},error:{}",id,response.getError());
            throw new JsonResponseException(response.getError());
        }
        Warehouse exist = response.getResult();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(exist.getId());
        warehouse.setIsMpos(0);
        Response<Boolean> res = warehouseWriteService.update(warehouse);
        if(!res.isSuccess()){
            log.error("update warehouse failed,error:{}",res.getError());
            throw new JsonResponseException(res.getError());
        }
        warehousePusher.removeWarehouses(exist.getCompanyId(),exist.getExtra().get("outCode"));

        //刷新缓存
        warehouseCacher.refreshById(exist.getId());
    }

    /**
     * @param id 仓库id
     * @param safeStock 安全库存
     */
    @ApiOperation("设置安全库存")
    @RequestMapping(value = "/{id}/safestock/setting",method = RequestMethod.PUT)
    public void setSafeStock(@PathVariable Long id,@RequestParam Integer safeStock){
        log.info("SET-WAREHOUSE-SAFE-STOCK id:{} safe stock to:{}",id,safeStock);
        Response<Warehouse> response = warehouseReadService.findById(id);
        if(!response.isSuccess()){
            log.error("fail to find warehouse by id:{},error:{}",id,response.getError());
            throw new JsonResponseException(response.getError());
        }
        Warehouse exist = response.getResult();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(exist.getId());
        Map<String,String> extra = exist.getExtra();
        extra.put(TradeConstants.WAREHOUSE_SAFESTOCK,safeStock.toString());
        warehouse.setExtra(extra);
        Response<Boolean> res = warehouseWriteService.update(warehouse);
        if(!res.isSuccess()){
            log.error("update warehouse failed,error:{}",res.getError());
            throw new JsonResponseException(res.getError());
        }
        //刷新缓存
        warehouseCacher.refreshById(exist.getId());

    }

    /**
     *
     * @param warehouseId 仓库id
     */
    @ApiOperation("设置虚拟店以及退货仓")
    @RequestMapping(value = "/{warehouseId}/server/info",method = RequestMethod.PUT)
    public void updateWarehouseServerInfo(@PathVariable Long warehouseId,@RequestBody WarehouseServerInfo warehouseServerInfo){
        Response<Warehouse> res = warehouseReadService.findById(warehouseId);
        if(!res.isSuccess()){
            log.error("fail to find warehouse by id {}",warehouseId);
            throw new JsonResponseException(res.getError());
        }
        Warehouse exist = res.getResult();
        Map<String,String> extra = exist.getExtra();
        Map<String,String> tags = exist.getTags() == null ? Maps.newHashMap() : exist.getTags();
        extra.put(TradeConstants.WAREHOUSE_VIRTUALSHOPCODE,warehouseServerInfo.getVirtualShopCode());
        extra.put(TradeConstants.WAREHOUSE_VIRTUALSHOPNAME,warehouseServerInfo.getVirtualShopName());
        tags.put(TradeConstants.WAREHOUSE_RETURNWAREHOUSECODE,warehouseServerInfo.getReturnWarehouseCode());
        tags.put(TradeConstants.WAREHOUSE_RETURNWAREHOUSENAME,warehouseServerInfo.getReturnWarehouseName());
        tags.put(TradeConstants.WAREHOUSE_RETURNWAREHOUSEID,warehouseServerInfo.getReturnWarehouseId()!=null?warehouseServerInfo.getReturnWarehouseId().toString():null);
        Warehouse update = new Warehouse();
        update.setId(warehouseId);
        update.setExtra(extra);
        update.setTags(tags);
        Response<Boolean> r = warehouseWriteService.update(update);
        if(!r.isSuccess()){
            log.error("fail to update warehouse id:{},cause:{}",warehouseId,r.getError());
            throw new JsonResponseException(r.getError());
        }

        //刷新缓存
        warehouseCacher.refreshById(exist.getId());
    }


    /**
     * @param warehouseIds 仓库id集合
     */
    @ApiOperation("仓库批量mpos打标")
    @RequestMapping(value = "/batch/make/flag",method = RequestMethod.PUT)
    public void batchMakeMposFlag(@RequestParam String warehouseIds){
        List<Long> ids  = Splitters.splitToLong(warehouseIds,Splitters.COMMA);
        for (Long id : ids) {
            makeMposFlag(id);
        }
    }

    /**
     * @param warehouseIds 仓库id集合
     */
    @ApiOperation("仓库批量mpos取消打标")
    @RequestMapping(value = "/batch/cancel/flag",method = RequestMethod.PUT)
    public void batchCancelMposFlag(@RequestParam String warehouseIds){
        List<Long> ids  = Splitters.splitToLong(warehouseIds,Splitters.COMMA);
        for (Long id : ids) {
            cancelMposFlag(id);
        }
    }

    /**
     * @param warehouseIds id集合
     * @param safeStock 安全库存
     */
    @ApiOperation("批量设置安全库存")
    @RequestMapping(value = "/batch/safestock/setting",method = RequestMethod.PUT)
    public void batchSetSafeStock(@RequestParam String warehouseIds,@RequestParam Integer safeStock){
        List<Long> ids = Splitters.splitToLong(warehouseIds,Splitters.COMMA);
        for (Long id:ids) {
            setSafeStock(id,safeStock);
        }
    }

    /**
     * @param warehouseIds 仓库id集合
     * @param warehouseServerInfo 服务信息
     */
    @ApiOperation("批量更新仓库服务信息")
    @RequestMapping(value = "/batch-set/server/info", method = RequestMethod.PUT)
    public void batchUpdateShopServerInfo(@RequestParam String warehouseIds, @RequestBody WarehouseServerInfo warehouseServerInfo) {
        List<Long> ids  = Splitters.splitToLong(warehouseIds,Splitters.COMMA);
        for (Long warehouseId : ids){
            this.updateWarehouseServerInfo(warehouseId,warehouseServerInfo);
        }
    }


    @ApiOperation("根据id获取仓库信息")
    @RequestMapping(value = "/{id}/query/by/cache", method = RequestMethod.GET)
    public Warehouse batchUpdateShopServerInfo(@PathVariable("id") Long warehouseId) {
        return warehouseCacher.findById(warehouseId);
    }



}
