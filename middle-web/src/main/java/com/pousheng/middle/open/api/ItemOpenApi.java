package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.ShopType;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.api.dto.SkuIsMposDto;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private WarehouseRulesClient warehouseRulesClient;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private GroupRuleCacherProxy GroupRuleCacherProxy;
    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    /**
     * 判断多个sku是否参与mpos
     *
     * @param barCodes 商品编码，多个用逗号隔开 ,约定单笔最大500个
     * @return 各个商品是否参与mpos
     */
    @OpenMethod(key = "check.sku.is.mpos.api", paramNames = {"barCodes", "companyId", "shopOuterCode"}, httpMethods = RequestMethod.POST)
    public List<SkuIsMposDto> helloWord(@NotEmpty(message = "barCodes.empty") String barCodes,
                                        @NotEmpty(message = "company.id.empty") String companyId,
                                        @NotEmpty(message = "shop.outer.code.empty") String shopOuterCode) {
        log.info("HK-CHECK-MPOS-START param barcodes is:{} companyId is:{} shopOuterCode is:{} ", barCodes, companyId, shopOuterCode);
        try {

            boolean isNumber = companyId.matches("[0-9]+");
            if (!isNumber) {
                log.error("company id:{} not number", companyId);
                throw new OPServerException(200, "company.id.non-numeric");
            }

            //2、查询店铺是否存在
            Shop currentShop;
            try {
                currentShop = middleShopCacher.findByOuterIdAndBusinessId(shopOuterCode,Long.valueOf(companyId));
                if (!com.google.common.base.Objects.equal(currentShop.getStatus(), 1)) {
                    log.info("shop({0}).status.abnormal", currentShop.getId());
                    throw new OPServerException(200,"shop.status.abnormal");
                }
                if (com.google.common.base.Objects.equal(currentShop.getType(), ShopType.RECEIVING_SHOP.value())) {
                    log.info("shop({0}).type.abnormal", currentShop.getId());
                    throw new OPServerException(200,"shop.type.abnormal");
                }
            }catch (ServiceException e){
                log.error("not find shop by outer id:{} business id:{}",shopOuterCode,companyId);
                throw new OPServerException(200,e.getMessage());
            }

            ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(currentShop.getExtra());
            Long openShopId = currentShopExtraInfo.getOpenShopId();
            if (Arguments.isNull(openShopId)) {
                log.error("shop(id:{}) not mapping open shop", currentShop.getId());
                throw new OPServerException(200, "shop.not.mapping.open.shop");
            }

            //3、查询门店的发货仓范围
            Response<List<Long>> warehouseIdsRes = warehouseRulesClient.findWarehouseIdsByShopId(openShopId);
            if(!warehouseIdsRes.isSuccess()){
                log.error("find warehouse rule item by shop id:{} fail,error:{}",openShopId,warehouseIdsRes.getError());
                throw new OPServerException(200,warehouseIdsRes.getError());
            }

            List<Long> warehouseIds = warehouseIdsRes.getResult();

            if (CollectionUtils.isEmpty(warehouseIds)) {
                log.error("not find warehouse rule item by shop id:{} fail",openShopId);
                throw new OPServerException(200,"not.find.warehouse.rule.item");

            }

            List<String> barcodeList = Splitters.COMMA.splitToList(barCodes);

            String templateName = "ps_search.mustache";
            Map<String, String> params = Maps.newHashMap();
            params.put("skuCodes", barCodes);
            //以查询的sku一定是中台已经有的前提
            Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> searchResp = skuTemplateSearchReadService.searchWithAggs(1, 500, templateName, params, SearchSkuTemplate.class);
            if (!searchResp.isSuccess()) {
                log.error("find sku template by barCodes:{} fail,error:{}", barcodeList, searchResp.getError());
                throw new OPServerException(200, searchResp.getError());
            }
            List<SearchSkuTemplate> skuTemplates = searchResp.getResult().getEntities().getData();
            if (!Objects.equals(skuTemplates.size(), barcodeList.size())) {
                log.error("some barcode:{} middle not exist", barcodeList);
                throw new OPServerException(200, "some.barcode.middle.not.exist");
            }
            Set<Long> groupIds = Sets.newHashSet(GroupRuleCacherProxy.findByShopId(openShopId));

            Map<String,Long> skuAndStockMap = queryStock(warehouseIds,barcodeList,openShopId);

            List<SkuIsMposDto> skuIsMposDtos = Lists.newArrayListWithCapacity(barcodeList.size());
            for (SearchSkuTemplate skuTemplate : skuTemplates) {
                SkuIsMposDto skuIsMposDto = new SkuIsMposDto();
                skuIsMposDto.setBarcode(skuTemplate.getSkuCode());
                skuIsMposDto.setIsMpos(isMopsItem(skuTemplate, groupIds));
                if (skuAndStockMap.containsKey(skuTemplate.getSkuCode())) {
                    skuIsMposDto.setStock(skuAndStockMap.get(skuTemplate.getSkuCode()));
                }
                skuIsMposDtos.add(skuIsMposDto);
            }
            if(log.isDebugEnabled()){
                log.debug("HK-CHECK-MPOS-END param barcodes is:{} companyId is:{} shopOuterCode is:{} ,res [{}]", barCodes,companyId,shopOuterCode,JsonMapper.nonEmptyMapper().toJson(skuIsMposDtos));
            }
            return skuIsMposDtos;
        } catch (Exception e) {
            log.error("find mpos sku codes failed,caused by {}", e.getCause());
            throw new OPServerException(200, e.getMessage());
        }
    }


    private Map<String,Long> queryStock(List<Long> warehouseIds,List<String> skuCodesList, Long shopId){

        Multimap<String, Integer> stockBySkuCode = HashMultimap.create();

        Table<Long, String, Integer> skuCodeQuantityTable = HashBasedTable.create();
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(warehouseIds,skuCodesList,shopId);
        dispatchComponent.completeTab(skuStockInfos,skuCodeQuantityTable);

        makeStockByStock(stockBySkuCode,skuCodesList,skuCodeQuantityTable);


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


    private Boolean isMopsItem(SearchSkuTemplate skuTemplate, Set<Long> groupIds) {
        if (CollectionUtils.isEmpty(skuTemplate.getGroupIds()) || CollectionUtils.isEmpty(groupIds)) {
            return false;
        }
        Set<Long> result = Sets.newHashSet();
        result.addAll(groupIds);
        result.retainAll(skuTemplate.getGroupIds());
        if (result.size() > 0) {
            return true;
        }
        return false;
    }


    private void makeStockByStock(Multimap<String, Integer> stockBySkuCode, List<String> skuCodesList,
                                  Table<Long, String, Integer> skuCodeTable) {

        for (Long shopId : skuCodeTable.rowKeySet()) {
            for (String skuCode : skuCodesList) {
                if (skuCodeTable.contains(shopId, skuCode)) {
                    stockBySkuCode.put(skuCode, skuCodeTable.get(shopId, skuCode));
                }
            }
        }
    }

}
