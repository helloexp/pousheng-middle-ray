package com.pousheng.middle.hksyc.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private WarehouseSkuReadService warehouseSkuReadService;
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
        }else {
            log.warn("do query stock info for:{} ,stock type:{} skip,because stockCodes is empty",skuCodes,stockType);
            //新版查询库存必须传仓库范围，所以如果这里没有范围则直接返回空列表
            return Lists.newArrayList();
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
            if(Objects.equals(2,Integer.valueOf(skuStockInfo.getStock_type()))){
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
                    if(!Objects.equals(shop.getStatus(),1)){
                        log.warn("current shop(id:{}) status:{} invalid,so skip",shop.getId(),shop.getStatus());
                        continue;
                    }
                    skuStockInfo.setBusinessId(shop.getId());
                    skuStockInfo.setBusinessName(shop.getName());
                    //商品必须打标为mpos标签才可以参与门店发货
                    filterIsMposSku(skuStockInfo,middleStockList);

                }catch (Exception e){
                    log.error("find shop by outer id:{} fail,cause:{}",skuStockInfo.getStock_code(),Throwables.getStackTraceAsString(e));
                }

            }
        }

        return middleStockList;
    }


    //只有打标为mpos标签的货品才可以参与派单
    private void filterIsMposSku(HkSkuStockInfo skuStockInfo,List<HkSkuStockInfo> middleStockList){

        List<HkSkuStockInfo.SkuAndQuantityInfo> materialList = skuStockInfo.getMaterial_list();
        List<HkSkuStockInfo.SkuAndQuantityInfo> newMaterialList = Lists.newArrayListWithCapacity(materialList.size());

        List<String> skuCodes = Lists.transform(materialList, new Function<HkSkuStockInfo.SkuAndQuantityInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HkSkuStockInfo.SkuAndQuantityInfo input) {
                return input.getBarcode();
            }
        });

        Response<List<SkuTemplate>> listRes = skuTemplateReadService.findBySkuCodes(skuCodes);
        if( !listRes.isSuccess()){
            log.error("find sku template by sku codes:{} fail,error:{}",skuCodes,listRes.getError());
            //这里将查询失败当做非mpos商品处理（严格意义上这里应该要报错的）
            return;
        }
        List<SkuTemplate> skuTemplates = listRes.getResult();

        if (CollectionUtils.isEmpty(skuTemplates)){
            log.error("not find sku template by sku codes:{} ",skuCodes);
            //这里将查询不到的当做非mpos商品处理（严格意义上这里应该要报错的）
            return ;
        }

        Map<String, SkuTemplate> skuInfoMap = skuTemplates.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));

        for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : materialList) {
            SkuTemplate skuTemplate = skuInfoMap.get(skuAndQuantityInfo.getBarcode());
            if (Arguments.isNull(skuTemplate)){
                log.error("not find sku template by sku code:{}",skuAndQuantityInfo.getBarcode());
                continue;
            }
            if (Objects.equals(skuTemplate.getType(),2)){
                newMaterialList.add(skuAndQuantityInfo);
            }
        }

        skuStockInfo.setMaterial_list(newMaterialList);
        middleStockList.add(skuStockInfo);

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

    /**
     * 〈根据仓库ID和skucode查询商品库存信息〉
     *
     * @param warehouseIds 仓库ID
     * @param skuCodes sku
     * @return: 商品库存信息集合
     * Author:xiehong
     * Date: 2018/6/20 下午5:48
     */
    public List<HkSkuStockInfo> doQueryStockInfo(List<Long> warehouseIds, List<String> skuCodes){
        if (CollectionUtils.isEmpty(warehouseIds) || CollectionUtils.isEmpty(skuCodes)){
            log.error("warehouseIds or skuCodes is null");
            return Lists.newArrayList();
        }
        //TODO 单测
        List<HkSkuStockInfo> hkSkuStockInfos = Lists.newArrayListWithExpectedSize(warehouseIds.size());
        for (final Long warehouseId : warehouseIds){
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            if (null == warehouse){
                log.warn("find warehouse by id {} is null",warehouseId);
                continue;
            }
            Map<String,String> extra = warehouse.getExtra();
            if (MapUtils.isEmpty(extra)){
                log.warn("find warehouse by id {} extra  is null",warehouseId);
                continue;
            }
            String outCode = extra.get("outCode");
            if (StringUtils.isEmpty(outCode)){
                log.warn("find warehouse by id {} outCode  is null",warehouseId);
                continue;
            }
            List<String> codes= Splitter.on("-").omitEmptyStrings().trimResults().splitToList(warehouse.getCode());
            String company_id = codes.get(0);
            String stock_id = codes.get(1);
            Long businessId = warehouseId;
            String businessName = warehouse.getName();
            Integer type = warehouse.getType();
            HkSkuStockInfo info = new HkSkuStockInfo();
            if (Objects.equals(WarehouseType.SHOP_WAREHOUSE.value(),type)){
                //获取shop
                Shop shop = middleShopCacher.findByOuterIdAndBusinessId(outCode,Long.valueOf(company_id));
                businessId = shop.getId();
                businessName = shop.getName();
                info.setStock_type("1");
            } else {
                info.setStock_type("2");
            }
            info.setBusinessId(businessId);
            info.setBusinessName(businessName);
            info.setCompany_id(company_id);
            info.setStock_id(stock_id);
            info.setStock_code(outCode);
            info.setStock_name(warehouse.getName());
            List<HkSkuStockInfo.SkuAndQuantityInfo> material_list = Lists.newArrayList();
            info.setMaterial_list(material_list);

            //获取库存
            Response<List<WarehouseSkuStock>> resp = warehouseSkuReadService.findSkuStocks(warehouseId,skuCodes);
            if (!resp.isSuccess() ||  CollectionUtils.isEmpty(resp.getResult())){
                log.error("find warehouse {} sku [{}] stock fail {}",warehouseId,skuCodes.toString(),resp.getError());
                continue;
            }
            List<WarehouseSkuStock> warehouseSkuStocks = resp.getResult();
            Map<String,WarehouseSkuStock> skuStockMap = warehouseSkuStocks.stream().filter(Objects::nonNull).collect(Collectors.toMap(WarehouseSkuStock::getSkuCode,a->a));
            List<String> stockSkuCodes = skuStockMap.keySet().stream().collect(Collectors.toList());

            //获取
            Response<List<SkuTemplate>> listRes = skuTemplateReadService.findBySkuCodes(stockSkuCodes);
            if( !listRes.isSuccess()){
                log.error("find sku template by sku codes:{} fail,error:{}",skuCodes,listRes.getError());
               continue;
            }
            List<SkuTemplate> skuTemplates = listRes.getResult();

            if (CollectionUtils.isEmpty(skuTemplates)){
                log.error("not find sku template by sku codes:{} ",skuCodes);
               continue;
            }
            Map<String, SkuTemplate> skuTemplateMap = skuTemplates.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));

            for (String c : codes){
                WarehouseSkuStock stock = skuStockMap.get(c);
                SkuTemplate temp = skuTemplateMap.get(c);
                if (null != stock && null != temp){
                    //如果是店仓则商品必须打标为mpos商品才可以参与发货
                    if (Objects.equals(warehouse.getType(),WarehouseType.SHOP_WAREHOUSE.value())){
                        if (!Objects.equals(temp.getType(),2)){
                            continue;
                        }
                    }

                    Map<String,String> tempExtra = temp.getExtra();
                    if (MapUtils.isNotEmpty(tempExtra)){
                        String materialId = tempExtra.get("materialId");
                        HkSkuStockInfo.SkuAndQuantityInfo skuquantity = new HkSkuStockInfo.SkuAndQuantityInfo();
                        skuquantity.setBarcode(c);
                        skuquantity.setMaterial_id(materialId);
                        skuquantity.setMaterial_name(temp.getName());
                        skuquantity.setQuantity(Integer.valueOf(String.valueOf(stock.getAvailStock())));
                        material_list.add(skuquantity);
                    } else {
                        log.warn("warehouse {} sku {} SkuTemplate {} extra is null ",warehouseId,c,temp.getId());
                    }
                }
            }

            hkSkuStockInfos.add(info);

        }
        return hkSkuStockInfos;
    }



    }