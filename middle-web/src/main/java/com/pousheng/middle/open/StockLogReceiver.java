package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.item.dto.IndexedStockLog;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.item.service.StockLogDumpService;
import com.pousheng.middle.open.stock.yunju.YunjuErrorCodeEnum;
import com.pousheng.middle.open.stock.yunju.dto.StockItem;
import com.pousheng.middle.open.stock.yunju.dto.StockPushLogStatus;
import com.pousheng.middle.open.stock.yunju.dto.YjStockReceiptRequest;
import com.pousheng.middle.open.stock.yunju.dto.YjStockReceiptResponse;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import com.pousheng.middle.web.middleLog.dto.InventoryTradeLog;
import com.pousheng.middle.web.middleLog.dto.StockLogDto;
import com.pousheng.middle.web.middleLog.dto.StockLogTypeEnum;
import de.danielbechler.util.Objects;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/8/16
 */
@Component
@Slf4j
@MQConsumer
@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "true", matchIfMissing = false)
public class StockLogReceiver {

    @Autowired
    private StockLogDumpService stockLogDumpService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private MiddleStockPushLogWriteService middleStockPushLogWriteSerive;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;


    @MQSubscribe(topic = "stockLogTopic", consumerGroup = "stockLogGroup",
            consumeMode = ConsumeMode.CONCURRENTLY)
    public void pushLog(StockLogDto dto) {
        log.info("start to push hk to middle  stock logs to ES/DB，type={}", dto.getType());
        makeLogs(dto);
        log.info("END to push hk to middle  stock logs to ES/DB");
    }

    private List<IndexedStockLog> makeLogs(StockLogDto dto) {
        StockLogTypeEnum type = StockLogTypeEnum.from(dto.getType());
        List<IndexedStockLog> logs = Lists.newArrayList();
        switch (type) {
            case HKTOMIDDLE:
                logs = makeHKToMiddle(logs, dto.getLogJson());
                stockLogDumpService.batchDump(logs);
                break;
            case MIDDLETOSHOP:
                makeMiddleToShop(dto.getLogJson());
                break;
            case TRADE:
                logs = makeTradeLog(logs, dto.getLogJson());
                stockLogDumpService.batchDump(logs);
                break;
            case YUNJUTOMIDDLE:
                makeYunjuReceiverLog(dto.getLogJson());
            default:
                log.error("incorrect stock log type");

        }
        return logs;
    }

    private void makeYunjuReceiverLog(String logJson) {
        long startTime = System.currentTimeMillis();
        YjStockReceiptRequest request = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(logJson,YjStockReceiptRequest.class);
        List<StockPushLog> stockPushLogs = new ArrayList<>();
        try {
            String requestNo = request.getSerialNo();
            String error = request.getError();
            String errorInfo = request.getErrorInfo();

            if(Objects.isEqual(error, YunjuErrorCodeEnum.PARTIAL_FAILURE.value())){
                //部分失败
                List<StockItem> items = request.getItems();
                items.forEach(item -> {

                    String lineNo = item.getLineNo();
                    int status = YunjuErrorCodeEnum.SUCCESS.value().equals(item.getError()) ? StockPushLogStatus.DEAL_SUCESS.value() : StockPushLogStatus.DEAL_FAIL.value();
                    String cause = item.getErrorInfo();
                    StockPushLog pushLog = StockPushLog.builder()
                            .requestNo(requestNo)
                            .lineNo(lineNo)
                            .status(status)
                            .cause(cause)
                            .build();
                    stockPushLogs.add(pushLog);
                });
                middleStockPushLogWriteService.batchUpdateResultByRequestIdAndLineNo(stockPushLogs);
            }else {
                //全部成功/失败
                StockPushLog pushLog = StockPushLog.builder()
                        .requestNo(requestNo)
                        .status(Objects.isEqual(error,YunjuErrorCodeEnum.SUCCESS.value())?StockPushLogStatus.DEAL_SUCESS.value():StockPushLogStatus.DEAL_FAIL.value())
                        .cause(errorInfo)
                        .build();
                middleStockPushLogWriteService.updateStatusByRequest(pushLog);
            }
        }catch (JsonResponseException | ServiceException e) {
            log.error("failed to update yunju stock receipt(json={}),error:{}", logJson, e.getMessage());
        }catch (Exception e){
            log.error("failed to update yunju stock receipt(json={}),cause:{} ", logJson, Throwables.getStackTraceAsString(e));
        }
        if(log.isDebugEnabled()) {
            log.debug("YUN-JIT-STOCK-PUSH-RESULT-DEAL,param:{},cost:{}", logJson,  System.currentTimeMillis() - startTime);
        }

    }

    /**
     * 将恒康推送中台的记录转成标准格式
     *
     * @param logs
     * @param logJson
     * @return
     */
    private List<IndexedStockLog> makeHKToMiddle(List<IndexedStockLog> logs, String logJson) {
        List<StockDto> stockDtos = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(logJson, JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class, StockDto.class));
        List<String> skuCodes = stockDtos.stream().map(e -> e.getSkuCode()).collect(Collectors.toList());
        Map<String, String> materialMap = getMaterial(skuCodes);
        for (StockDto dto : stockDtos) {
            WarehouseDTO warehouseDTO = warehouseCacher.findById(dto.getWarehouseId());
            logs.add(new IndexedStockLog().quantity(dto.getQuantity()).skuCode(dto.getSkuCode()).materialId(materialMap.get(dto.getSkuCode()))
                    .warehouseCode(warehouseDTO.getOutCode()).warehouseName(warehouseDTO.getWarehouseName())
                    .createdAt(dto.getUpdatedAt()).type(StockLogTypeEnum.HKTOMIDDLE.value()));
        }
        return logs;
    }

    /**
     * 中台推店铺仅存入db
     *
     * @param logJson
     */
    private void makeMiddleToShop(String logJson) {
        List<StockPushLog> stockPushLogs = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(logJson, JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class, StockPushLog.class));
        List<String> skuCodes = stockPushLogs.stream().map(e -> e.getSkuCode()).collect(Collectors.toList());
        Map<String, String> materialMap = getMaterial(skuCodes);
        stockPushLogs.forEach(e -> e.setMaterialId(materialMap.get(e.getSkuCode())));
        middleStockPushLogWriteSerive.creates(stockPushLogs);
    }

    /**
     * 将交易产生的库存变更日志转成标准格式
     *
     * @param logs
     * @param logJson
     * @return
     */
    private List<IndexedStockLog> makeTradeLog(List<IndexedStockLog> logs, String logJson) {
        List<InventoryTradeLog> inventoryTradeLogs = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(logJson, JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class, InventoryTradeLog.class));
        List<String> skuCodes = inventoryTradeLogs.stream().map(e -> e.getSkuCode()).collect(Collectors.toList());
        Map<String, String> materialMap = getMaterial(skuCodes);
        for (InventoryTradeLog dto : inventoryTradeLogs) {
            WarehouseDTO warehouseDTO = warehouseCacher.findByCode(dto.getWarehouseCode());
            String shipmentId;
            if (dto.getShipmentId().startsWith("SAL")) {
                shipmentId = dto.getShipmentId();
            } else {
                shipmentId = "SHP" + dto.getShipmentId();
            }
            logs.add(new IndexedStockLog().skuCode(dto.getSkuCode()).materialId(materialMap.get(dto.getSkuCode()))
                    .shipmentId(shipmentId).quantity(dto.getQuantity().intValue())
                    .warehouseCode(warehouseDTO.getOutCode()).warehouseName(warehouseDTO.getWarehouseName())
                    .createdAt(dto.getCreatedAt()).operate(dto.getType())
                    .type(StockLogTypeEnum.TRADE.value()));
        }
        return logs;
    }


    private Map<String, String> getMaterial(List<String> skuCodes) {
        Map<String, String> map = new HashMap<>();
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCodes", Joiners.COMMA.join(skuCodes));
        Response<WithAggregations<SearchSkuTemplate>> response =
                skuTemplateSearchReadService.doSearchWithAggs(1, skuCodes.size(), templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("fail to find by skuCodes，param={},cause:{}", params, response.getError());
            return map;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        if (response.getResult().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)) {
            return map;
        }
        map = response.getResult().getData().stream().filter(e -> e.getSpuCode() != null).collect(Collectors.toMap(SearchSkuTemplate::getSkuCode, SearchSkuTemplate::getSpuCode));
        return map;
    }
}
