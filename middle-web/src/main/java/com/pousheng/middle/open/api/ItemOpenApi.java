package com.pousheng.middle.open.api;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.PsItemTool;
import com.pousheng.middle.open.api.dto.SkuIsMposDto;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2018/1/16
 */
@OpenBean
@Slf4j
public class ItemOpenApi {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private PsShopReadService psShopReadService;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseRuleReadService warehouseRuleReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;

    /**
     * 判断多个sku是否参与mpos
     * @param barCodes 商品编码，多个用逗号隔开 ,约定单笔最大500个
     * @return 各个商品是否参与mpos
     */
    @OpenMethod(key = "check.sku.is.mpos.api", paramNames = {"barCodes","companyId","shopOuterCode"}, httpMethods = RequestMethod.POST)
    public List<SkuIsMposDto> helloWord(@NotEmpty(message = "barCodes.empty") String barCodes,
                                        @NotEmpty(message = "company.id.empty") String companyId,
                                        @NotEmpty(message = "shop.outer.code.empty") String shopOuterCode) {
        log.info("HK-CHECK-MPOS-START param barcodes is:{} companyId is:{} shopOuterCode is:{} ", barCodes,companyId,shopOuterCode);
        try{

            boolean isNumber=companyId.matches("[0-9]+");
            if(!isNumber){
                log.error("company id:{} not number",companyId);
                throw new OPServerException(200,"company.id.non-numeric");
            }

            Response<Optional<Shop>> response = psShopReadService.findByOuterIdAndBusinessId(shopOuterCode,Long.valueOf(companyId));
            if(!response.isSuccess()){
                log.error("find shop by outer id:{} business id:{} fail,error:{}",shopOuterCode,companyId,response.getError());
                throw new OPServerException(200,response.getError());
            }
            Optional<Shop> shopOptional = response.getResult();
            if(!shopOptional.isPresent()){
                log.error("not find shop by outer id:{} business id:{}",shopOuterCode,companyId);
                throw new OPServerException(200,"shop.not.exist");
            }



            //2、查询店铺是否存在
            Shop currentShop;
            try {
                 currentShop = middleShopCacher.findByOuterIdAndBusinessId(shopOuterCode,Long.valueOf(companyId));
            }catch (ServiceException e){
                log.error("not find shop by outer id:{} business id:{}",shopOuterCode,companyId);
                throw new OPServerException(200,e.getMessage());

            }
            ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(currentShop.getExtra());
            Long openShopId = currentShopExtraInfo.getOpenShopId();
            if(Arguments.isNull(openShopId)){
                log.error("shop(id:{}) not mapping open shop",currentShop.getId());
                throw new OPServerException(200,"shop.not.mapping.open.shop");
            }

            //3、查询门店的发货仓范围
            Response<List<Long>> warehouseIdsRes = warehouseRuleReadService.findWarehouseIdsByShopId(openShopId);
            if(!warehouseIdsRes.isSuccess()){
                log.error("find warehouse rule item by shop id:{} fail,error:{}",openShopId,warehouseIdsRes.getError());
                throw new OPServerException(200,warehouseIdsRes.getError());
            }

            List<Long> warehouseIds = warehouseIdsRes.getResult();

            if(CollectionUtils.isEmpty(warehouseIds)){
                log.error("not find warehouse rule item by shop id:{} fail");
                throw new OPServerException(200,"not.find.warehouse.rule.item");

            }
            List<Warehouse> warehouseList = Lists.newArrayListWithCapacity(warehouseIds.size());
            for (Long warehouseId : warehouseIds){
                try {
                    warehouseList.add(warehouseCacher.findById(warehouseId));
                }catch (Exception e){
                    log.error("find warehouse by id:{} fail,cause:{}",warehouseId, Throwables.getStackTraceAsString(e));
                }
            }



            Map<String, Warehouse> stockCodeWarehosueMap = Maps.newHashMap();
            for (Warehouse warehouse : warehouseList){
                Map<String, String>  extra = warehouse.getExtra();
                if(CollectionUtils.isEmpty(extra)||!extra.containsKey("outCode")){
                    log.error("warehouse(id:{}) out code invalid",warehouse.getId());
                    throw new OPServerException(200,"warehouse.out.code.invalid");
                }
                String outCode =  extra.get("outCode");
                stockCodeWarehosueMap.put(outCode,warehouse);
            }


            List<String> barcodeList = Splitters.COMMA.splitToList(barCodes);

            //以查询的sku一定是中台已经有的前提
            Response<List<SkuTemplate>> skuTemplatesRes = skuTemplateReadService.findBySkuCodes(barcodeList);
            if(!skuTemplatesRes.isSuccess()){
                log.error("find sku template by barcodes:{} fail,error:{}",barcodeList,skuTemplatesRes.getError());
                throw new OPServerException(200,skuTemplatesRes.getError());
            }
            List<SkuTemplate> skuTemplates = skuTemplatesRes.getResult().stream().filter(skuTemplate -> !Objects.equals(skuTemplate.getStatus(),-3)).collect(Collectors.toList());

            if(!Objects.equals(skuTemplates.size(),barcodeList.size())){
                log.error("some barcode:{} middle not exist",barcodeList);
                throw new OPServerException(200,"some.barcode.middle.not.exist");
            }

            Map<String,Long> skuAndStockMap = queryStock(stockCodeWarehosueMap,barcodeList);

            List<SkuIsMposDto> skuIsMposDtos = Lists.newArrayListWithCapacity(barcodeList.size());
            for (SkuTemplate skuTemplate : skuTemplates){
                SkuIsMposDto skuIsMposDto = new SkuIsMposDto();
                skuIsMposDto.setBarcode(skuTemplate.getSkuCode());
                skuIsMposDto.setIsMpos(PsItemTool.isMopsItem(skuTemplate));
                if(skuAndStockMap.containsKey(skuTemplate.getSkuCode())){
                    skuIsMposDto.setStock(skuAndStockMap.get(skuTemplate.getSkuCode()));
                }
                skuIsMposDtos.add(skuIsMposDto);
            }

            log.info("HK-CHECK-MPOS-END");
            return skuIsMposDtos;
        }catch (Exception e){
            log.error("find mpos sku codes failed,caused by {}", e.getCause());
            throw new OPServerException(200, e.getMessage());
        }
    }


    private Map<String,Long> queryStock(Map<String, Warehouse> stockCodeWarehosueMap,List<String> skuCodesList){

        Multimap<String, Integer> stockBySkuCode = HashMultimap.create();

        List<String> shopCodes = Lists.newArrayListWithCapacity(stockCodeWarehosueMap.size());
        List<String> warehouseCodes = Lists.newArrayListWithCapacity(stockCodeWarehosueMap.size());

        for (String outCode: stockCodeWarehosueMap.keySet()){
            Warehouse warehouse = stockCodeWarehosueMap.get(outCode);
            if(Objects.equals(warehouse.getType(),0)){
                warehouseCodes.add(outCode);
            }else {
                shopCodes.add(outCode);
            }
        }

        //门店
        Table<Long, String, Integer> shopSkuCodeQuantityTable = HashBasedTable.create();
        List<HkSkuStockInfo> shopSkuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(shopCodes,skuCodesList,1);
        dispatchComponent.completeShopTab(shopSkuStockInfos,shopSkuCodeQuantityTable);

        makeStockByStock(stockBySkuCode,skuCodesList,shopSkuCodeQuantityTable);

        //仓
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = HashBasedTable.create();
        List<HkSkuStockInfo> warehouseSkuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(warehouseCodes,skuCodesList,2);
        dispatchComponent.completeWarehouseTab(warehouseSkuStockInfos,warehouseSkuCodeQuantityTable);

        makeStockByStock(stockBySkuCode,skuCodesList,warehouseSkuCodeQuantityTable);

        Map<String,Long> skuAndStockMap = Maps.newHashMapWithExpectedSize(stockBySkuCode.size());

        for (String skuCode : stockBySkuCode.keySet()){
            Long totalStock =0L;
            for (Integer stock : stockBySkuCode.get(skuCode)){
                totalStock+=stock;

            }
            skuAndStockMap.put(skuCode,totalStock);
        }

        return skuAndStockMap;


    }

    private void makeStockByStock(Multimap<String, Integer> stockBySkuCode,List<String> skuCodesList,
                                                      Table<Long, String, Integer> skuCodeTable){

        for (Long shopId : skuCodeTable.rowKeySet()){
            for (String skuCode: skuCodesList){
                if(skuCodeTable.contains(shopId,skuCode)){
                    stockBySkuCode.put(skuCode,skuCodeTable.get(shopId,skuCode));
                }
            }
        }
    }

}
