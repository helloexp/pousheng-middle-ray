package com.pousheng.middle.web.events.trade.listener.export;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.*;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.search.stock.StockSendRuleDTO;
import com.pousheng.middle.search.stock.StockSendSearchComponent;
import com.pousheng.middle.shop.enums.ShopType;
import com.pousheng.middle.web.export.ExportService;
import com.pousheng.middle.web.export.StockSendRuleEntity;
import com.pousheng.middle.web.utils.export.ExportContext;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-19 16:19<br/>
 */
@Slf4j
@Component
public class StockSendExport {
    private final ExportService exportService;
    private final StockSendSearchComponent stockSendSearchComponent;

    private final int pageSize = 1000;

    public StockSendExport(ExportService exportService, StockSendSearchComponent stockSendSearchComponent) {
        this.exportService = exportService;
        this.stockSendSearchComponent = stockSendSearchComponent;
    }

    public void export(StockSendCriteria criteria, Long userId) {
        List<StockSendRuleEntity> exportData = new ArrayList<>();
        criteria.setPageSize(pageSize);

        String fileName;
        if (CollectionUtils.isEmpty(criteria.getZoneIds())) {
            fileName = "线上店铺对应发货仓";
        } else {
            fileName = "线下区部对应发货仓";
        }

        Map<Long, List<StockSendRuleDTO>> listMultimap = Maps.newHashMap();
        List<StockSendRuleDTO> resultData = Lists.newArrayList();
        //根据区部分批查询
        if (CollectionUtils.isNotEmpty(criteria.getZoneIds())) {
            List<Long> zoneIds = criteria.getZoneIds();
            criteria.setZoneIds(null);
            for (Long zoneId : zoneIds) {
                criteria.setZoneId(zoneId);
                List<StockSendRuleDTO> stockSendRuleDTOS = queryData(criteria);
                if (stockSendRuleDTOS.size() > 0) {
                    resultData.addAll(stockSendRuleDTOS);
                }
            }
        } else if (CollectionUtils.isNotEmpty(criteria.getShopIds())) {
            //根据门店分批查询
            List<Long> shopIds = criteria.getShopIds();
            criteria.setShopIds(null);
         //   log.info("门店:{}", shopIds);
            for (Long shopId : shopIds) {
                criteria.setShopId(shopId);
                List<StockSendRuleDTO> stockSendRuleDTOS = queryData(criteria);
                if (stockSendRuleDTOS.size() > 0) {
                    resultData.addAll(stockSendRuleDTOS);
                }
            }
        } else {
            //单个的查询
            List<StockSendRuleDTO> stockSendRuleDTOS = queryData(criteria);
            if (stockSendRuleDTOS.size() > 0) {
                resultData.addAll(stockSendRuleDTOS);
            }
        }


        if (resultData != null && resultData.size() > 0) {
            for (StockSendRuleDTO stockSendRuleDTO : resultData) {
                Long key = stockSendRuleDTO.getRuleId();
                List<StockSendRuleDTO> stockSendRuleDTOsList = listMultimap.get(key);
                if (stockSendRuleDTOsList != null) {
                    stockSendRuleDTOsList.add(stockSendRuleDTO);
                    listMultimap.put(key, stockSendRuleDTOsList);
                } else {
                    listMultimap.put(key, Lists.newArrayList(stockSendRuleDTO));
                }
            }
          //  log.info("数据:{}", JSONObject.toJSONString(listMultimap));
            Set<Long> keys = listMultimap.keySet();
            for (Long key : keys) {
            //    log.info("key:{}", key);
                List<StockSendRuleDTO> stockSendRuleDTOS = listMultimap.get(key);
                if (stockSendRuleDTOS != null && stockSendRuleDTOS.size() > 0) {
                    Set<String> shopNameList = stockSendRuleDTOS.stream().map(s -> s.getShopName()).collect(Collectors.toSet());
                    String shopName = String.join(",", shopNameList);
                    //过滤掉不同门店相同仓库导致的数据重复问题
                    Map<String, StockSendRuleDTO> warehouseData = Maps.newHashMap();
                    for (StockSendRuleDTO stock : stockSendRuleDTOS) {
                        String mapKey = stock.getWarehouseCompanyCode() + stock.getWarehouseOutCode();
                        warehouseData.put(mapKey, stock);
                    }

                    Collection<StockSendRuleDTO> warehouseValues = warehouseData.values();
                    for (StockSendRuleDTO stock : warehouseValues) {
                        StockSendRuleEntity entity = new StockSendRuleEntity();
                        entity.setRuleId(key);
                        entity.setShopName(shopName);
                        entity.setWarehouseCompanyId(stock.getWarehouseCompanyCode());
                        entity.setWarehouseOutCode(stock.getWarehouseOutCode());
                        entity.setWarehouseName(stock.getWarehouseName());

                        Optional.ofNullable(stock.getWarehouseType())
                                .map(ShopType::from)
                                .map(ShopType::getDesc)
                                .ifPresent(entity::setWarehouseType);

                        if (stock.getWarehouseStatus() != null) {
                            entity.setWarehouseStatus(Objects.equals(1, stock.getWarehouseStatus()) ? "开启" : "关闭");
                        }
                        exportData.add(entity);
                    }
                }
            }
        }

        if (exportData.isEmpty()) {
            throw new JsonResponseException("export.data.empty");
        }
     //   log.info("待导出数据:{}", JSONObject.toJSONString(exportData));
        exportService.saveToDiskAndCloud(new ExportContext(fileName + ".xlsx", exportData), userId);
    }


    public List<StockSendRuleDTO> queryData(StockSendCriteria criteria) {
        int pageNo = 1;
        List<StockSendRuleDTO> queryData = Lists.newArrayList();
        while (true) {
            criteria.setPageNo(pageNo);
            Paging<StockSendRuleDTO> paging = stockSendSearchComponent.search(criteria);
            List<StockSendRuleDTO> pagingData = paging.getData();
            if (paging.isEmpty()) {
                break;
            }
            queryData.addAll(pagingData);
            //如果是数据少于1000条，那么不需要查询下一次了
            if (pagingData.size() < pageSize) {
                break;
            }
            pageNo += 1;
        }
        return queryData;
    }
}
