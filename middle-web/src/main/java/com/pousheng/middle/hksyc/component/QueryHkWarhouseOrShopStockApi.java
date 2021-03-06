package com.pousheng.middle.hksyc.component;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsItemGroupType;
import com.pousheng.middle.item.enums.ShopType;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.web.item.cacher.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class QueryHkWarhouseOrShopStockApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final Splitter DASH = Splitter.on("-").omitEmptyStrings().trimResults();

    @Autowired
    private ErpClient erpClient;

    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private InventoryClient inventoryClient;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private GroupRuleCacherProxy groupRuleCacherProxy;
    @Autowired
    private ShopGroupRuleCacher shopGroupRuleCacher;
    @Autowired
    private WarehouseGroupRuleCacher warehouseGroupRuleCacher;
    @Autowired
    private ItemGroupCacherProxy itemGroupCacherProxy;
    @Autowired
    private ItemGroupCacher itemGroupCacher;
    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @Autowired
    private OpenShopCacher openShopCacher;

    private static String COMPANY_CODE = "companyCode";

    private static final TypeReference<List<HkSkuStockInfo>> LIST_OF_SKU_STOCK = new TypeReference<List<HkSkuStockInfo>>() {};


    /**
     * 查询库存信息
     *
     * @param stockCodes 仓或门店外码集合
     * @param skuCodes   商品编码集合
     * @param stockType  0 = 不限; 1 = 店仓; 2 = 总仓
     */
    public List<HkSkuStockInfo> doQueryStockInfo(List<String> stockCodes, List<String> skuCodes, Integer stockType) {
        Map<String, String> map = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(stockCodes)) {
            map.put("stock_codes", Joiners.COMMA.join(stockCodes));
        } else {
            log.warn("do query stock info for:{} ,stock type:{} skip,because stockCodes is empty", skuCodes, stockType);
            //新版查询库存必须传仓库范围，所以如果这里没有范围则直接返回空列表
            return Lists.newArrayList();
        }
        if (CollectionUtils.isEmpty(skuCodes)) {
            log.error("sku code info invalid");
            throw new ServiceException("sku.code.invalid");
        }
        map.put("barcodes", Joiners.COMMA.join(skuCodes));
        map.put("stock_type", stockType.toString());
        log.info("[QUERY-STOCK]query hk stock request param:{}", map);
        String responseBody = erpClient.get("common/erp/base/countmposinstock", map);
        log.info("[QUERY-STOCK]query hk stock success result:{}", responseBody);
        //当查询商品的在各个门店后仓的库存未0，则返回接口为空
        //当查询某个门店中的三个sku，如果只有两个sku有库存则只返回这两个有库存的sku
        List<HkSkuStockInfo> hkSkuStockInfoList = readStockFromJson(responseBody);

        List<HkSkuStockInfo> middleStockList = Lists.newArrayListWithCapacity(hkSkuStockInfoList.size());

        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfoList) {
            if (Objects.equals(2, Integer.valueOf(skuStockInfo.getStock_type()))) {
                try {
                    WarehouseDTO warehouse = warehouseCacher.findByCode(skuStockInfo.getCompany_id() + "-" + skuStockInfo.getStock_id());
                    skuStockInfo.setBusinessId(warehouse.getId());
                    skuStockInfo.setBusinessName(warehouse.getWarehouseName());
                    middleStockList.add(skuStockInfo);

                } catch (Exception e) {
                    log.error("find warehouse by company id:{} and stock id:{} fail,cause:{}", skuStockInfo.getCompany_id(), skuStockInfo.getStock_code(), Throwables.getStackTraceAsString(e));
                }
            } else {
                try {
                    Shop shop = middleShopCacher.findByOuterIdAndBusinessId(skuStockInfo.getStock_code(), Long.valueOf(skuStockInfo.getCompany_id()));
                    //过滤掉已冻结或已删除的店铺
                    if (!Objects.equals(shop.getStatus(), 1)) {
                        log.warn("current shop(id:{}) status:{} invalid,so skip", shop.getId(), shop.getStatus());
                        continue;
                    }
                    skuStockInfo.setBusinessId(shop.getId());
                    skuStockInfo.setBusinessName(shop.getName());
                    //商品必须打标为mpos标签才可以参与门店发货
                    ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                    Long openShopId = currentShopExtraInfo.getOpenShopId();
                    if (Arguments.isNull(openShopId)) {
                        log.error("shop(id:{}) not mapping open shop", shop.getId());
                        throw new JsonResponseException("shop.not.mapping.open.shop");
                    }
                    filterIsMposSku(skuStockInfo, middleStockList, openShopId);

                } catch (Exception e) {
                    log.error("find shop by outer id:{} fail,cause:{}", skuStockInfo.getStock_code(), Throwables.getStackTraceAsString(e));
                }

            }
        }

        return middleStockList;
    }


    //只有打标为mpos标签的货品才可以参与派单
    private void filterIsMposSku(HkSkuStockInfo skuStockInfo, List<HkSkuStockInfo> middleStockList, Long openShopId) {

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
        if (!listRes.isSuccess()) {
            log.error("find sku template by sku codes:{} fail,error:{}", skuCodes, listRes.getError());
            //这里将查询失败当做非mpos商品处理（严格意义上这里应该要报错的）
            return;
        }
        List<SkuTemplate> skuTemplates = listRes.getResult();

        if (CollectionUtils.isEmpty(skuTemplates)) {
            log.error("not find sku template by sku codes:{} ", skuCodes);
            //这里将查询不到的当做非mpos商品处理（严格意义上这里应该要报错的）
            return;
        }

        Map<String, SkuTemplate> skuInfoMap = skuTemplates.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));

        for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : materialList) {
            SkuTemplate skuTemplate = skuInfoMap.get(skuAndQuantityInfo.getBarcode());
            if (Arguments.isNull(skuTemplate)) {
                log.error("not find sku template by sku code:{}", skuAndQuantityInfo.getBarcode());
                continue;
            }
            if (isVendible(skuTemplate.getSkuCode(), openShopId)) {
                newMaterialList.add(skuAndQuantityInfo);
            }
        }

        skuStockInfo.setMaterial_list(newMaterialList);
        middleStockList.add(skuStockInfo);

    }


    public Boolean isVendible(String skuCode, Long openShopId) {
        Set<Long> groupIds = Sets.newHashSet(groupRuleCacherProxy.findByShopId(openShopId));
        log.info("query shop group ,shopId{}, groupIds", openShopId, groupIds);
        if (CollectionUtils.isEmpty(groupIds)) {
            return false;
        }
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCode", skuCode);
        params.put("groupIds", Joiners.COMMA.join(groupIds));
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by materialId:{} and size:{} fail,error:{}", skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult().getTotal() != 0;

    }


    public List<Long> isVendibleWarehouse(String skuCode, List<Long> warehouseIds, String companyCode) {
        List<Long> vendible = Lists.newArrayList();
        WithAggregations<SearchSkuTemplate> result = searchSkuTemplate(skuCode);
        if (result.getTotal() == 0) {
            return vendible;
        }
        for (Long warehouseId : warehouseIds) {
            try {
                WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
                if (null == warehouse) {
                    log.warn("find warehouse by id {} is null", warehouseId);
                    continue;
                }
                if (StringUtils.isEmpty(warehouse.getOutCode())) {
                    log.warn("find warehouse by id {} outCode  is null", warehouseId);
                    continue;
                }
                if (canDeliveryForStock(result.getData().get(0), warehouse, companyCode, Boolean.FALSE)) {
                    vendible.add(warehouseId);
                }
            } catch (Exception e) {
                log.error("failed to find shop type and businessInfo for warehouse (id:{}),case:{}",
                        warehouseId, Throwables.getStackTraceAsString(e));
                continue;
            }
        }
        return vendible;
    }


    public Boolean canDeliveryForStock(SearchSkuTemplate skuTemplate, WarehouseDTO warehouse, String companyCode, Boolean logEnable) {
        if (logEnable) {
            log.info("skuTemplate {} ,warehouse_company {} ,companyCode {}", skuTemplate, warehouse.getCompanyId(), companyCode);
        }
        Set<Long> groupIds;
        if (Objects.equals(warehouse.getWarehouseSubType(), WarehouseType.SHOP_WAREHOUSE.value())) {
            //获取shop
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.parseLong(warehouse.getCompanyId()));
            ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            Long openShopId = currentShopExtraInfo.getOpenShopId();
            if (Arguments.isNull(openShopId)) {
                log.error("shop(id:{}) not mapping open shop", shop.getId());
                return false;
            }
            groupIds = Sets.newHashSet(shopGroupRuleCacher.findByShopId(openShopId));
        } else {
            groupIds = Sets.newHashSet(warehouseGroupRuleCacher.findByWarehouseId(warehouse.getId()));
        }
        if (logEnable) {
            log.info("find warehouse id {} , groupIds {}", warehouse.getId(), groupIds);
        }
        if (CollectionUtils.isEmpty(groupIds)) {
            return false;
        }
        if (CollectionUtils.isEmpty(skuTemplate.getGroupIds())) {
            return false;
        }
        //查询商品和店铺的交集，查看是否存在，
        Set<Long> result = Sets.newHashSet();
        result.addAll(groupIds);
        result.retainAll(skuTemplate.getGroupIds());
        if (result.size() == 0) {
            return false;
        }
        //如果已经是同公司就不需要再去判断了 如果存在且是全国的 则返回true
        if (warehouse.getCompanyId().equals(companyCode)) {
            return true;
        }
        if (logEnable) {
            log.warn("find union  groupIds {}", result);
        }
        for (Long id : result) {
            if (PsItemGroupType.ALL.value().equals(itemGroupCacher.findById(id).getType())) {
                return true;
            }
        }
        return false;

    }


    private List<HkSkuStockInfo> readStockFromJson(String json) {

        if (Strings.isNullOrEmpty(json)) {
            log.warn("not query stock from hk");
            return Lists.newArrayList();
        }

        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(json, LIST_OF_SKU_STOCK);
        } catch (IOException e) {
            log.error("analysis json:{} to stock dto error,cause:{}", json, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();

    }

    /**
     * 〈根据仓库ID和skucode查询商品库存信息〉
     *
     * @param warehouseIds 仓库ID
     * @param skuCodes     sku
     * @param withSafe     是否使用安全库存，如果不使用的话 直接忽略库存为0的记录
     * @return: 商品库存信息集合
     * Author:xiehong
     * Date: 2018/6/20 下午5:48
     */
    public List<HkSkuStockInfo> doQueryStockInfo(List<Long> warehouseIds, List<String> skuCodes, Long shopId, Object... withSafe) {
        if (CollectionUtils.isEmpty(warehouseIds) || CollectionUtils.isEmpty(skuCodes)) {
            log.error("warehouseIds or skuCodes is null");
            return Lists.newArrayList();
        }
        // 去重
        skuCodes = skuCodes.stream().distinct().collect(Collectors.toList());

        //获取
        Response<List<SkuTemplate>> listRes = skuTemplateReadService.findBySkuCodes(skuCodes);
        if (!listRes.isSuccess()) {
            log.error("find sku template by sku codes:{} fail,error:{}", skuCodes, listRes.getError());
            return Lists.newArrayList();
        }
        List<SkuTemplate> skuTemplates = listRes.getResult();
        if (CollectionUtils.isEmpty(skuTemplates)) {
            log.error("not find sku template by sku codes:{} ", skuCodes);
            return Lists.newArrayList();
        }
        Map<String, SearchSkuTemplate> searchSkuTemplateMap = new HashMap<>();
        for (SkuTemplate sku : skuTemplates) {
            searchSkuTemplateMap.put(sku.getSkuCode(), searchSkuTemplate(sku.getSkuCode()).getData().get(0));
        }
        Map<String, SkuTemplate> skuTemplateMap = skuTemplates.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));

        List<AvailableInventoryDTO> availableInvList;
        if (withSafe.length == 0) {
            Response<List<AvailableInventoryDTO>> availableInvRes = inventoryClient.getAvailableInventory(
                    dispatchComponent.getAvailInvReq(warehouseIds, skuCodes), shopId);
            if (!availableInvRes.isSuccess() || CollectionUtils.isEmpty(availableInvRes.getResult())) {
                log.warn("not skuStockInfos so skip");
                return Lists.newArrayList();
            }
            availableInvList = availableInvRes.getResult().stream().filter(e -> e.getTotalAvailQuantity() > 0).collect(Collectors.toList());
        } else {
            Response<List<AvailableInventoryDTO>> availableInvRes = inventoryClient.getAvailableInventoryWithoutSupply(
                    dispatchComponent.getAvailInvReq(warehouseIds, skuCodes), shopId);
            if (!availableInvRes.isSuccess() || CollectionUtils.isEmpty(availableInvRes.getResult())) {
                log.warn("not skuStockInfos so skip");
                return Lists.newArrayList();
            }
            availableInvList = availableInvRes.getResult();
        }
        OpenShop openShop = openShopCacher.findById(shopId);
        String companyCode = openShop.getExtra().get(COMPANY_CODE);
        if (StringUtils.isEmpty(companyCode)) {
            companyCode = DASH.splitToList(openShop.getAppKey()).get(0);
        }
        //TODO 单测
        List<HkSkuStockInfo> hkSkuStockInfos = Lists.newArrayListWithExpectedSize(warehouseIds.size());
        for (final Long warehouseId : warehouseIds) {
            WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
            if (null == warehouse) {
                log.warn("find warehouse by id {} is null", warehouseId);
                continue;
            }
            if (StringUtils.isEmpty(warehouse.getOutCode())) {
                log.warn("find warehouse by id {} outCode  is null", warehouseId);
                continue;
            }

            List<AvailableInventoryDTO> availableInv = availableInvList.stream()
                    .filter(dto -> warehouseId.equals(dto.getWarehouseId())).collect(Collectors.toList());
            if (ObjectUtils.isEmpty(availableInv)) {
                log.warn("no inventory available for warehouse: {}", warehouseId);
                continue;
            }

            List<String> codes = DASH.splitToList(warehouse.getWarehouseCode());
            String company_id = codes.get(0);
            String stock_id = codes.get(1);
            Long businessId = warehouseId;
            String businessName = warehouse.getWarehouseName();
            Integer type = warehouse.getWarehouseSubType();
            HkSkuStockInfo info = new HkSkuStockInfo();
            if (Objects.equals(WarehouseType.SHOP_WAREHOUSE.value(), type)) {
                //获取shop
                Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.valueOf(company_id));
                if (!Objects.equals(shop.getStatus(), 1) || Objects.equals(shop.getType(), ShopType.ORDERS_SHOP.value())) {
                    continue;
                }
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
            info.setStock_code(warehouse.getOutCode());
            info.setStock_name(warehouse.getWarehouseName());
            List<HkSkuStockInfo.SkuAndQuantityInfo> material_list = Lists.newArrayList();
            info.setMaterial_list(material_list);

            // 获取库存
            // sku 编码可能重复
            Map<String, AvailableInventoryDTO> skuStockMap = availableInv.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(AvailableInventoryDTO::getSkuCode, a -> a));

            List<String> stockSkuCodes = new ArrayList<>(skuStockMap.keySet());
            for (String c : stockSkuCodes) {
                AvailableInventoryDTO stock = skuStockMap.get(c);
                SkuTemplate temp = skuTemplateMap.get(c);
                if (null != stock && null != temp) {
                    //当是自动派单时间 无论是什么类型的仓库 都要去检查能否发货 手工派单则忽略
                    if (withSafe.length == 0 && !canDeliveryForStock(searchSkuTemplateMap.get(c), warehouse, companyCode, Boolean.TRUE)) {
                        continue;
                    }
                    Map<String, String> tempExtra = temp.getExtra();
                    if (MapUtils.isNotEmpty(tempExtra)) {
                        String materialId = tempExtra.get("materialId");
                        HkSkuStockInfo.SkuAndQuantityInfo skuquantity = new HkSkuStockInfo.SkuAndQuantityInfo();
                        skuquantity.setBarcode(c);
                        skuquantity.setMaterial_id(materialId);
                        skuquantity.setMaterial_name(temp.getName());
                        skuquantity.setQuantity(Integer.valueOf(String.valueOf(stock.getTotalAvailQuantity())));
                        skuquantity.setQuantityWithOutSafe(stock.getAvailableQuantityWithoutSafe());
                        material_list.add(skuquantity);
                    } else {
                        log.warn("warehouse {} sku {} SkuTemplate {} extra is null ", warehouseId, c, temp.getId());
                    }
                }
            }

            hkSkuStockInfos.add(info);

        }

        log.info("query inventory available quantity for dispatch, result: {}", JSON.toJSONString(hkSkuStockInfos));

        return hkSkuStockInfos;
    }


    private WithAggregations<SearchSkuTemplate> searchSkuTemplate(String skuCode) {
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCode", skuCode);
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by skuCode:{} fail,error:{}", skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getTotal() == 0) {
            throw new JsonResponseException("sku.template.find.fail");
        }
        return response.getResult();
    }

    /**
     * 验证可售仓库
     *
     * @param skuCode
     * @param warehouseIds
     * @param companyCode
     * @return
     */
    public List<Long> validateVendibleWarehouse(String skuCode, List<Long> warehouseIds, String companyCode) {
        List<Long> vendible = Lists.newArrayList();
        WithAggregations<SearchSkuTemplate> result = validateSkuTemplate(skuCode);
        if (result.getTotal() == 0) {
            return vendible;
        }
        for (Long warehouseId : warehouseIds) {
            try {
                WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
                if (null == warehouse) {
                    log.warn("find warehouse by id {} is null", warehouseId);
                    continue;
                }
                if (StringUtils.isEmpty(warehouse.getOutCode())) {
                    log.warn("find warehouse by id {} outCode  is null", warehouseId);
                    continue;
                }
                if (canDeliveryForStock(result.getData().get(0), warehouse, companyCode, Boolean.FALSE)) {
                    vendible.add(warehouseId);
                }
            } catch (Exception e) {
                log.error("failed to find shop type and businessInfo for warehouse (id:{}),case:{}",
                        warehouseId, Throwables.getStackTraceAsString(e));
                continue;
            }
        }
        return vendible;
    }

    private WithAggregations<SearchSkuTemplate> validateSkuTemplate(String skuCode) {
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCode", skuCode);
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by skuCode:{} fail,error:{}", skuCode, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

}
