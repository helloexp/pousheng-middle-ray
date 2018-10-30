package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.warehouses.dto.StockPushLogCriteria;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseCacher warehouseCacher;


    @RequestMapping(value = "/create/push/log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void createStockPushLog(@RequestBody StockPushLog stockPushLog) {
        Response<Long> response = middleStockPushLogWriteService.create(stockPushLog);
        if (!response.isSuccess()) {
            log.error("fffff");
        }
    }

    /**
     * 根据主键查询推送日志
     *
     * @param id 表的主键
     * @return
     */
    @RequestMapping(value = "/stock/push/log/by/id", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<StockPushLog> queryStockPushLogById(@RequestParam("id") Long id) {
        Response<StockPushLog> r = middleStockPushLogReadSerive.findById(id);
        if (!r.isSuccess()) {
            log.error("failed to query stockPushLog with is:{}, error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r;
    }

    /**
     * 分页查询库存推送日志
     *
     * @param criteria   查询条件
     * @return
     */
    @RequestMapping(value = "/stock/push/log/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<StockPushLog>> paginationStockPushLog(StockPushLogCriteria criteria) {

        if (criteria.getEndAt() != null) {
            criteria.endAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        Response<Paging<StockPushLog>> r = middleStockPushLogReadSerive.pagination(criteria.getPageNo(), criteria.getPageSize(), criteria.toMap());
        if (!r.isSuccess()) {
            log.error("failed to pagination stockPushLog with params:{}, error code:{}", criteria, r.getError());
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


    /**
     * 在一个仓库中对应sku的库存
     * 如果是店仓则要减掉中台已经占用的
     *
     * @param skuCodes sku codes, 以','分割
     * @return sku在对应仓库中的可用库存情况
     */
    @RequestMapping(value = "/{warehouseId}/stocks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Integer> findStocksForSkus(@PathVariable("warehouseId") Long warehouseId,
                                                  @RequestParam("skuCodes") String skuCodes,
                                                  @RequestParam("shopId") Long shopId) {

        List<String> skuCodeList = Lists.newArrayList(Splitters.COMMA.splitToList(skuCodes));
        HashMap<String, Integer> map = new HashMap<>(4);
        if (CollectionUtils.isEmpty(skuCodeList)) {
            return map;
        }
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(Lists.newArrayList(warehouseId), skuCodeList, shopId, Boolean.TRUE);
        if (skuStockInfos.size() == 0) {
            return Collections.emptyMap();
        }
        for (HkSkuStockInfo skuStockInfo : skuStockInfos) {
            if (skuStockInfo.getMaterial_list().size() == 0) {
                continue;
            }
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : skuStockInfo.getMaterial_list()) {
                log.info("skuCode is {},quantity is{}", skuAndQuantityInfo.getBarcode(), skuAndQuantityInfo.getQuantityWithOutSafe());
                map.put(skuAndQuantityInfo.getBarcode(), skuAndQuantityInfo.getQuantityWithOutSafe());
            }
        }
        return map;
    }


    @GetMapping(value = "/{id}")
    public WarehouseDTO findById(@PathVariable("id") Long id) {
        return warehouseCacher.findById(id);
    }

}
