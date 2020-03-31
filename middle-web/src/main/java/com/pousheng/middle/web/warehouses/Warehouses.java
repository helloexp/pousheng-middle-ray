package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogReadSerive;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.warehouses.component.MiddleWarehouseService;
import com.pousheng.middle.web.warehouses.dto.AfterSaleWarehouseRequest;
import com.pousheng.middle.web.warehouses.dto.CommonChooseWarehouse;
import com.pousheng.middle.web.warehouses.dto.SendWarehouseDTO;
import com.pousheng.middle.web.warehouses.dto.SendWarehouseRequest;
import com.pousheng.middle.web.warehouses.dto.StockPushLogCriteria;
import com.pousheng.middle.web.warehouses.dto.StockPushLogDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@RestController
@RequestMapping("/api/warehouse")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE)
@Api("中台库存")
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

    @Autowired
    private WarehouseClient warehouseClient;

    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private MiddleWarehouseService middleWarehouseService;


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
    public Response<Paging<StockPushLogDto>> paginationStockPushLog(StockPushLogCriteria criteria) {

        if (criteria.getEndAt() != null) {
            criteria.endAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        Response<Paging<StockPushLog>> r = middleStockPushLogReadSerive.pagination(criteria.getPageNo(), criteria.getPageSize(), criteria.toMap());
        if (!r.isSuccess()) {
            log.error("failed to pagination stockPushLog with params:{}, error code:{}", criteria, r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<StockPushLogDto> result = Lists.newArrayList();
        // 渲染出错码，数据库保存出错码，页面渲染让用户看到，渲染一般放在controller层比较好
        if (!ObjectUtils.isEmpty(r.getResult().getData())) {
            Locale locale = LocaleContextHolder.getLocale();
            for (StockPushLog log : r.getResult().getData()) {
                if (!StringUtils.isEmpty(log.getCause())) {
                    log.setCause(messageSource.getMessage(log.getCause(), null, log.getCause(), locale));
                }
                StockPushLogDto stockPushLog = new StockPushLogDto();
                stockPushLog.setId(log.getId());
                stockPushLog.setShopId(log.getShopId());
                stockPushLog.setOutId(log.getOutId());
                stockPushLog.setShopName(log.getShopName());
                stockPushLog.setWarehouseId(log.getWarehouseId());
                stockPushLog.setWarehouseName(log.getWarehouseName());
                stockPushLog.setWarehouseOuterCode(log.getWarehouseOuterCode());
                stockPushLog.setSkuCode(log.getSkuCode());
                stockPushLog.setChannelSkuId(log.getChannelSkuId());
                stockPushLog.setMaterialId(log.getMaterialId());
                stockPushLog.setStatus(log.getStatus());
                stockPushLog.setCause(log.getCause());
                stockPushLog.setQuantity(String.valueOf(log.getQuantity()));
                OpenShop openShop = openShopCacher.findById(log.getShopId());
                if (openShop != null) {//银泰渠道 出入库单调整数量
                    if (MiddleChannel.from(openShop.getChannel()) == MiddleChannel.YINTAI && !Strings.isNullOrEmpty(log.getRequestNo())) {
                        stockPushLog.setQuantity(String.valueOf(log.getQuantity())+"("+log.getRequestNo()+")");
                    }
                }
//                stockPushLog.setRequestNo(log.getRequestNo());
                stockPushLog.setLineNo(log.getLineNo());
                stockPushLog.setSyncAt(log.getSyncAt());
                stockPushLog.setCreatedAt(log.getCreatedAt());
                stockPushLog.setUpdatedAt(log.getUpdatedAt());
                result.add(stockPushLog);
            }
        }

        return Response.ok(new Paging<>(r.getResult().getTotal(), result));
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
        HashMap<String, Integer> map = new HashMap<>();
        if (CollectionUtils.isEmpty(skuCodeList)) {
            return map;
        }

        OpenShop openShop = openShopCacher.findById(shopId);
        if (checkFlag(openShop)) {
            //增加限制
            WarehouseDTO warehouseDTO = warehouseCacher.findById(warehouseId);
            CommonChooseWarehouse request = new CommonChooseWarehouse();//这里要公司+外码定位唯一的仓库
            request.setCompanyCode(warehouseDTO.getCompanyCode());
            request.setOutCode(warehouseDTO.getOutCode());
            for (String skuCode : skuCodeList) {
                Response<Paging<SendWarehouseDTO>> sendWarehouseResp = middleWarehouseService.pagingSendWarehouse(shopId, skuCode, request);
                if (!sendWarehouseResp.isSuccess() || sendWarehouseResp.getResult().isEmpty()) {
                    throw new JsonResponseException(messageSource.getMessage("sku.{0}.send.warehouse.fail", new String[]{skuCode}, skuCode + "不允许被派发至当前仓库", LocaleContextHolder.getLocale()));
                }
                SendWarehouseDTO dto = sendWarehouseResp.getResult().getData().get(0);
                if (!dto.getIsAvailable()) {
                    throw new JsonResponseException(messageSource.getMessage("sku.{0}.send.warehouse.fail", new String[]{skuCode}, skuCode + "不允许被派发至当前仓库", LocaleContextHolder.getLocale()));
                }
                map.put(skuCode, dto.getQuantity());
            }
        } else {
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
        }

        return map;
    }

    private boolean checkFlag(OpenShop openShop) {
        String checkFlag = openShop.getExtra().get(TradeConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG);
        if (!"1".equals(checkFlag)) {
            return false;
        }
        String ruleFlag = openShop.getExtra().get(TradeConstants.MANUAL_DISPATCH_RULE_CONTROL);
        return "true".equals(ruleFlag);
    }


    @GetMapping(value = "/{id}")
    public WarehouseDTO findById(@PathVariable("id") Long id) {
        return warehouseCacher.findById(id);
    }


    @ApiOperation("根据名称或者外码获取仓库信息")
    @RequestMapping(value = "/paging/by/name", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseDTO> pagingByOutCodeOrName(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                              @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                              @RequestParam(required = false, value = "name") String namePrefix,
                                                              @RequestParam(required = false, value ="shopId") Long shopId) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("nameOrCodePrefix", namePrefix);

        if (Objects.isNull(shopId)) {
            return pagingWarehouse(pageNo, pageSize, namePrefix, null);
        }
        Response<OpenShop> response = openShopReadService.findById(shopId);
        boolean checkDefaultWarehouseFlag = false;
        if (response.isSuccess()
            && !Objects.isNull(response.getResult())) {
            Map<String, String> extra = response.getResult().getExtra();
            // 若存在检查默认发货仓的标志位且为1 则设置标志位为true
            if (!Objects.isNull(extra)
                && "1".equals(extra.get(ShopConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG))) {
                checkDefaultWarehouseFlag = true;
            }
        }

        //检查店铺的默认发货仓
        if (checkDefaultWarehouseFlag) {
            return pagingWarehouse(pageNo, pageSize, namePrefix, shopId);
        }
        return pagingWarehouse(pageNo, pageSize, namePrefix, null);
    }

    private Paging<WarehouseDTO> pagingWarehouse(Integer pageNo, Integer pageSize, String namePrefix, Long shopId) {
        Response<Paging<WarehouseDTO>> response = warehouseClient.pagingBy(pageNo, pageSize, namePrefix, shopId);
        if (response.isSuccess()) {
            return response.getResult();
        }
        return new Paging<WarehouseDTO>();
    }

    @ApiOperation("手工派单选择可发货仓")
    @GetMapping(value = "/send/paging", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouse(SendWarehouseRequest request) {
        if (request.getShopId() != null && !Strings.isNullOrEmpty(request.getSkuCode())) {
            return middleWarehouseService.pagingSendWarehouseByShopIdAndSkuCode(request);
        } else if (request.getSkuOrderId() != null) {
            return middleWarehouseService.pagingSendWarehouseBySkuOrderId(request);
        }
        return Response.fail("required.is.null");

    }

    @ApiOperation("售后换货选择可发货仓")
    @GetMapping(value = "/aftersale/send/paging", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouseForAfterSale(AfterSaleWarehouseRequest request) {
        if (Strings.isNullOrEmpty(request.getOrderCode()) || Strings.isNullOrEmpty(request.getSkuCode())) {
            return Response.fail("orderCodeOrSkuCode.is.null");
        }
        return middleWarehouseService.pagingSendWarehouseByOrderCodeAndSkuCode(request);
    }
}
