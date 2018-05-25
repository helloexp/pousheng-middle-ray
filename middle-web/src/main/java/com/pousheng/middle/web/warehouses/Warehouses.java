package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
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
    private MiddleStockPushLogReadSerive middleStockPushLogReadSerive;
    @RpcConsumer
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;
    @Autowired
    private MessageSource messageSource;


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

        // 渲染出错码，数据库保存出错码，页面渲染让用户看到，渲染一般放在controller层比较好
        if (!ObjectUtils.isEmpty(r.getResult().getData())) {
            Locale locale = LocaleContextHolder.getLocale();
            for (StockPushLog log : r.getResult().getData()) {
                if (!StringUtils.isEmpty(log.getCause())) {
                    log.setCause(messageSource.getMessage(log.getCause(), null, log.getCause(), locale));
                }
            }
        }


        return r;
    }


}
