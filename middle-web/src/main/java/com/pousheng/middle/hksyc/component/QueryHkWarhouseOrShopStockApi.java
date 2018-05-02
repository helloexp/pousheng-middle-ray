package com.pousheng.middle.hksyc.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
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
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
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
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;


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
        log.info("[QUERY-STOCK]query hk stock request param:{}",map);
        String responseBody = erpClient.get("common/erp/base/countmposinstock",map);
        log.info("[QUERY-STOCK]query hk stock success result:{}",responseBody);
        //当查询商品的在各个门店后仓的库存未0，则返回接口为空
        //当查询某个门店中的三个sku，如果只有两个sku有库存则只返回这两个有库存的sku
        List<HkSkuStockInfo> hkSkuStockInfoList =  readStockFromJson(responseBody);

        List<HkSkuStockInfo> middleStockList =  Lists.newArrayListWithCapacity(hkSkuStockInfoList.size());

        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfoList){
            if(Objects.equal(2,Integer.valueOf(skuStockInfo.getStock_type()))){
                try {
                    Warehouse warehouse = warehouseCacher.findByCode(skuStockInfo.getCompany_id()+"-"+skuStockInfo.getStock_id());
                    skuStockInfo.setBusinessId(warehouse.getId());
                    skuStockInfo.setBusinessName(warehouse.getName());
                    middleStockList.add(skuStockInfo);

                }catch (Exception e){
                    log.error("find warehouse by company id:{} and stock id:{} fail,cause:{}",skuStockInfo.getCompany_id(),skuStockInfo.getStock_code(),Throwables.getStackTraceAsString(e));
                }
            }else {
                try {
                    Shop shop = middleShopCacher.findByOuterIdAndBusinessId(skuStockInfo.getStock_code(),Long.valueOf(skuStockInfo.getCompany_id()));
                    //过滤掉已冻结或已删除的店铺
                    if(!Objects.equal(shop.getStatus(),1)){
                        log.warn("current shop(id:{}) status:{} invalid,so skip",shop.getId(),shop.getStatus());
                        continue;
                    }
                    skuStockInfo.setBusinessId(shop.getId());
                    skuStockInfo.setBusinessName(shop.getName());
                    //商品必须打标为mpos标签才可以参与门店发货
                    if(checkIsMposSku(skuStockInfo)){
                        middleStockList.add(skuStockInfo);
                    }

                }catch (Exception e){
                    log.error("find shop by outer id:{} fail,cause:{}",skuStockInfo.getStock_code(),Throwables.getStackTraceAsString(e));
                }

            }
        }

        return middleStockList;
    }


    private Boolean checkIsMposSku(HkSkuStockInfo skuStockInfo){

        List<String> skuCodes = Lists.transform(skuStockInfo.getMaterial_list(), new Function<HkSkuStockInfo.SkuAndQuantityInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HkSkuStockInfo.SkuAndQuantityInfo input) {
                return input.getBarcode();
            }
        });

        Response<List<SkuTemplate>> listRes = skuTemplateReadService.findBySkuCodes(skuCodes);
        if(!listRes.isSuccess()){
            log.error("find sku template by sku codes:{} fail,error:{}",skuCodes,listRes.getError());
            //这里将查询失败当做非mpos商品处理（严格意义上这里应该要报错的）
            return Boolean.FALSE;
        }
        List<SkuTemplate> skuTemplates = listRes.getResult();

        if(CollectionUtils.isEmpty(skuTemplates)){
            log.error("not find sku template by sku codes:{} ",skuCodes);
            //这里将查询不到的当做非mpos商品处理（严格意义上这里应该要报错的）
            return Boolean.FALSE;
        }

        return Objects.equal(skuTemplates.get(0).getType(),2);

    }




    private List<HkSkuStockInfo> readStockFromJson(String json) {

        if(Strings.isNullOrEmpty(json)){
            log.warn("not query stock from hk");
            return Lists.newArrayList();
        }

        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(json, LIST_OF_SKU_STOCK);
        } catch (IOException e) {
            log.error("analysis json:{} to stock dto error,cause:{}",json, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();

    }
}
