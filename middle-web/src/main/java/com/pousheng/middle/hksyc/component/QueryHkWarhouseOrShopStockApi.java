package com.pousheng.middle.hksyc.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

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
        List<HkSkuStockInfo> list = Lists.newArrayList();

        if(CollectionUtils.isEmpty(stockCodes)){
            stockCodes = Lists.newArrayList();
            if(Objects.equal(2,stockType)){
                stockCodes.add("242-00000000000001");
                stockCodes.add("200-200000003");
                stockCodes.add("201-201000345");
            }else {
                stockCodes.add("SP110090");
                stockCodes.add("SP110093");
                stockCodes.add("20000028");
            }
        }

        for (String stockCode : stockCodes){
            HkSkuStockInfo hkSkuStockInfo = new HkSkuStockInfo();
            hkSkuStockInfo.setStock_id(stockCode);
            hkSkuStockInfo.setStock_name(stockCode);

            List<HkSkuStockInfo.SkuAndQuantityInfo> skuAndQuantityInfos = Lists.newArrayList();

            for (String skuCode : skuCodes){
                HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo = new HkSkuStockInfo.SkuAndQuantityInfo();
                skuAndQuantityInfo.setQuantity(0);
                skuAndQuantityInfo.setBarcode(skuCode);
                skuAndQuantityInfos.add(skuAndQuantityInfo);
            }
            hkSkuStockInfo.setMaterial_list(skuAndQuantityInfos);
            if(Objects.equal(2,stockType)){
                Warehouse warehouse = warehouseCacher.findByCode(stockCode);
                hkSkuStockInfo.setBusinessId(warehouse.getId());
                hkSkuStockInfo.setBusinessName(warehouse.getName());
            }else {
                Shop shop = middleShopCacher.findShopByOuterId(stockCode);
                hkSkuStockInfo.setBusinessId(shop.getId());
                hkSkuStockInfo.setBusinessName(shop.getName());
            }

            list.add(hkSkuStockInfo);
        }

        return list;

       /* String stock_ids = Joiners.COMMA.join(stockCodes);
        String material_ids = Joiners.COMMA.join(skuCodes);
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        Map<String, Object> params = Maps.newHashMap();
        params.put("stock_ids",stock_ids);
        params.put("material_ids",material_ids);
        params.put("stock_type",stockType);


        String paramJson = JsonMapper.nonEmptyMapper().toJson(params);
        log.info("paramJson:{}",paramJson);
        String gateway = hkGateway+"/commonerp/erp/sal/updateordercancelstatus";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                //.trustAllHosts().trustAllCerts()
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("query hk stock info result:{}",responseBody);

        return readStockFromJson(responseBody);*/
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
