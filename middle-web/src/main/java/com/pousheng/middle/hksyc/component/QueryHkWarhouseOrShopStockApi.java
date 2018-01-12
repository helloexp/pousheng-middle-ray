package com.pousheng.middle.hksyc.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
        map.put("barcodes",Joiners.COMMA.join(skuCodes));
        map.put("stock_type",stockType.toString());
        String responseBody = erpClient.get("common/erp/base/countmposinstock",map);
        List<HkSkuStockInfo> hkSkuStockInfoList =  readStockFromJson(responseBody);
        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfoList){
            /*if(Objects.equal(2,stockType)){
                Warehouse warehouse = warehouseCacher.findByCode(stockCode);
                skuStockInfo.setBusinessId(warehouse.getId());
                skuStockInfo.setBusinessName(warehouse.getName());
            }else {
                Shop shop = middleShopCacher.findShopByOuterId(stockCode);
                skuStockInfo.setBusinessId(shop.getId());
                skuStockInfo.setBusinessName(shop.getName());
            }*/
        }

        return hkSkuStockInfoList;
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
