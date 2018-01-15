package com.pousheng.middle.hksyc.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class QueryHkWarhouseOrShopStockApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ErpClient erpClient;

    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseReadService warehouseReadService;


    private static final TypeReference<List<HkSkuStockInfo>> LIST_OF_SKU_STOCK = new TypeReference<List<HkSkuStockInfo>>() {};


    /**
     * 查询库存信息
     * @param stockCodes 仓或门店外码集合
     * @param skuCodes 商品编码集合
     * @param stockType 0 = 不限; 1 = 店仓; 2 = 总仓
     */
    public List<HkSkuStockInfo> doQueryStockInfo(List<String> stockCodes, List<String> skuCodes, Integer stockType){
        Map<String,String> map= Maps.newHashMap();
        if(!CollectionUtils.isEmpty(stockCodes)){
            map.put("stock_codes",Joiners.COMMA.join(stockCodes));
        }
        if(CollectionUtils.isEmpty(skuCodes)){
            log.error("sku code info invalid");
            throw new ServiceException("sku.code.invalid");
        }
        map.put("barcodes",Joiners.COMMA.join(skuCodes));
        map.put("stock_type",stockType.toString());
        String responseBody = erpClient.get("common/erp/base/countmposinstock",map);
        log.info("query hk stock success result:{}",responseBody);
        List<HkSkuStockInfo> hkSkuStockInfoList =  readStockFromJson(responseBody);

        List<HkSkuStockInfo> middleStockList =  Lists.newArrayListWithCapacity(hkSkuStockInfoList.size());

        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfoList){
            if(Objects.equal(2,stockType)){
                //todo 待roger返回 company_id
               // String company_id = "";
               // Warehouse warehouse = warehouseCacher.findByCode(company_id+"-"+skuStockInfo.getStock_id());
                Response<List<Warehouse>> warehouseRes = warehouseReadService.findByFuzzyCode(skuStockInfo.getStock_code());
                if(!warehouseRes.isSuccess()){
                    log.error("find warehouse by fuzzy code:{} fail,error:{}",skuStockInfo.getStock_code(),warehouseRes.getError());
                    throw new ServiceException(warehouseRes.getError());
                }
                if(CollectionUtils.isEmpty(warehouseRes.getResult())){
                    log.error("not find warehouse by fuzzy code:{} fail",skuStockInfo.getStock_code());
                    continue;
                }
                Warehouse warehouse = warehouseRes.getResult().get(0);
                skuStockInfo.setBusinessId(warehouse.getId());
                skuStockInfo.setBusinessName(warehouse.getName());
            }else {
                Shop shop = middleShopCacher.findShopByOuterId(skuStockInfo.getStock_code());
                skuStockInfo.setBusinessId(shop.getId());
                skuStockInfo.setBusinessName(shop.getName());
            }
            middleStockList.add(skuStockInfo);
        }

        return middleStockList;
    }


    private List<HkSkuStockInfo> readStockFromJson(String json) {
        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(json, LIST_OF_SKU_STOCK);
        } catch (IOException e) {
            log.error("analysis json:{} to stock dto error,cause:{}",json, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();

    }
}
