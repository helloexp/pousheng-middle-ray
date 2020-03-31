package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.ShopType;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.impl.dao.StockRecordLogDao;
import com.pousheng.middle.order.model.StockRecordLog;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleQueryOneRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRulesDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.warehouses.dto.AfterSaleWarehouseRequest;
import com.pousheng.middle.web.warehouses.dto.CommonChooseWarehouse;
import com.pousheng.middle.web.warehouses.dto.SendWarehouseDTO;
import com.pousheng.middle.web.warehouses.dto.SendWarehouseRequest;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AUTHOR: zhangbin
 * ON: 2019/8/12
 */
@Slf4j
@Service
public class MiddleWarehouseService {

    private SkuOrderReadService skuOrderReadService;
    private OpenShopReadService openShopReadService;
    private WarehouseRulesClient warehouseRulesClient;
    private InventoryClient inventoryClient;
    private ShopOrderReadService shopOrderReadService;
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    private OpenShopCacher openShopCacher;
    private WarehouseClient warehousesClient;
    private PsShopReadService psShopReadService;
    private StockRecordLogDao stockRecordLogDao;

    @Autowired
    public MiddleWarehouseService(SkuOrderReadService skuOrderReadService,
                                  OpenShopReadService openShopReadService,
                                  WarehouseRulesClient warehouseRulesClient,
                                  InventoryClient inventoryClient,
                                  ShopOrderReadService shopOrderReadService,
                                  SkuTemplateSearchReadService skuTemplateSearchReadService,
                                  QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi,
                                  OpenShopCacher openShopCacher,
                                  WarehouseClient warehousesClient,
                                  PsShopReadService psShopReadService,
                                  StockRecordLogDao stockRecordLogDao) {
        this.skuOrderReadService = skuOrderReadService;
        this.openShopReadService = openShopReadService;
        this.warehouseRulesClient = warehouseRulesClient;
        this.inventoryClient = inventoryClient;
        this.shopOrderReadService = shopOrderReadService;
        this.skuTemplateSearchReadService = skuTemplateSearchReadService;
        this.queryHkWarhouseOrShopStockApi = queryHkWarhouseOrShopStockApi;
        this.openShopCacher = openShopCacher;
        this.warehousesClient = warehousesClient;
        this.psShopReadService = psShopReadService;
        this.stockRecordLogDao = stockRecordLogDao;
    }

    private LoadingCache<String, Optional<ShopSkuSupplyRulesDTO>> ruleCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<ShopSkuSupplyRulesDTO>>() {
                @Override
                public Optional<ShopSkuSupplyRulesDTO> load(@NotNull String key) {
                    String[] split = key.split("-");
                    Long shopId = Long.valueOf(split[0]);
                    String skuCode = split[1];
                    Response<ShopSkuSupplyRulesDTO> ruleResp = inventoryClient.queryFullShopSkuSupplyRule(
                            ShopSkuSupplyRuleQueryOneRequest.builder().shopId(shopId).skuCode(skuCode).status("ENABLE").build());
                    if (!ruleResp.isSuccess()) {
                        log.error("fail to find shop stock rule by shopId:({}), skuCode:({}), cause:({})", shopId, skuCode, ruleResp.getError());
                        return Optional.empty();
                    }
                    return Optional.ofNullable(ruleResp.getResult());
                }
            });

    /**
     * 手工派单页面，根据子单号选择可发货仓(历史兼容）
     * @param request skuOrderId必填
     * @return 可选仓库
     */
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouseBySkuOrderId(SendWarehouseRequest request) {
        //子单号，获取店铺和sku，是否管控开关
        Response<SkuOrder> skuOrderResp = skuOrderReadService.findById(request.getSkuOrderId());
        if (!skuOrderResp.isSuccess()) {
            log.error("fail to find sku order by id({}), cause:({})", request.getSkuOrderId(), skuOrderResp.getError());
            return Response.fail(skuOrderResp.getError());
        }

        SkuOrder skuOrder = skuOrderResp.getResult();
        Long shopId = skuOrder.getShopId();
        String skuCode = skuOrder.getSkuCode();
        return pagingSendWarehouse(shopId, skuCode, request);
    }

    /**
     * 根据店铺id和条码选择发货仓
     * 因为入口比较多，页面加载数据不一致，重构入参。
     * 页面加载com.pousheng.middle.web.order.CreateShipments#waitHandleSku(java.lang.Long, java.lang.Integer)
     * @param request shopId+skuCode必填
     * @return 可选仓库
     */
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouseByShopIdAndSkuCode(SendWarehouseRequest request) {
        return pagingSendWarehouse(request.getShopId(), request.getSkuCode(), request);
    }

    /**
     * 售后换货页面，根据订单编码和条码选择发货仓
     * @param request orderCode和skuCode必填
     * @return 可选仓库
     */
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouseByOrderCodeAndSkuCode(AfterSaleWarehouseRequest request) {
        Response<ShopOrder> orderResp = shopOrderReadService.findByOrderCode(request.getOrderCode());
        if (!orderResp.isSuccess()) {
            log.error("fail to find shop order by code({}), cause:({})", request.getOrderCode(), orderResp.getError());
            return Response.fail(orderResp.getError());
        }

        return pagingSendWarehouse(orderResp.getResult().getShopId(), request.getSkuCode(), request);
    }

    //选择发货仓
    public Response<Paging<SendWarehouseDTO>> pagingSendWarehouse(Long shopId, String skuCode ,CommonChooseWarehouse request) {
        Response<OpenShop> openShopResp = openShopReadService.findById(shopId);
        if (!openShopResp.isSuccess()) {
            log.error("fail to find open shop by id({}), cause:({})", shopId, openShopResp.getError());
            return Response.fail(openShopResp.getError());
        }
        //是否管控...
        boolean controlFlag = getManualDispatchRuleControl(openShopResp.getResult());

        //店铺获取默认规则发货仓
        Response<List<Long>> warehouseIdsResp = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
        if (!warehouseIdsResp.isSuccess()) {
            log.error("fail to find warehouses by shopId, shopId({}), cause:({})", shopId, warehouseIdsResp.getError());
            return Response.fail(warehouseIdsResp.getError());
        }
        List<Long> warehouseIds = warehouseIdsResp.getResult();
        if (CollectionUtils.isEmpty(warehouseIds)) {
            log.info("warehouseIds is empty by shopId ({})", shopId);
            return Response.ok(Paging.empty());
        }

        log.info("prepare param shopId({}), skuCode({}), controlFlag({}), warehouseIds({})", shopId, skuCode, controlFlag, warehouseIds);
        List<WarehouseDTO> warehouses = fillWarehouseDTO(warehouseIds);
        //过滤 仓库状态
        warehouses = filterWarehouses(warehouses);
        List<Long> filterWarehouseIds = warehouses.stream().map(WarehouseDTO::getId).collect(Collectors.toList());
        ///库存行
        Response<List<AvailableInventoryDTO>> inventoryResp = inventoryClient.getAvailableInventoryWithoutSupply(
                filterWarehouseIds.stream().map(warehouseId -> AvailableInventoryRequest.builder().skuCode(skuCode).warehouseId(warehouseId).build())
                        .collect(Collectors.toList()),
                shopId);
        if (!inventoryResp.isSuccess()) {
            log.error("fail to find inventory by skuCode({}), warehouseIds:({}), cause:({})", skuCode, filterWarehouseIds, inventoryResp.getError());
            return Response.fail(inventoryResp.getError());
        }

        List<AvailableInventoryDTO> availableInventoryList = inventoryResp.getResult();
        if (CollectionUtils.isEmpty(availableInventoryList)) {
            log.info("availableInventoryList is empty");
            return Response.ok(Paging.empty());
        }

        Map<Long, AvailableInventoryDTO> availableInventoryMapping = availableInventoryList.stream()
                .collect(Collectors.toMap(AvailableInventoryDTO::getWarehouseId, Function.identity()));
        //初始数据
        List<SendWarehouseDTO> sendWarehouseList = convert(warehouses, availableInventoryMapping, skuCode);
        //过滤可用库存0和安全库存为0的
        List<SendWarehouseDTO> filter = filterInventoryIsZero(sendWarehouseList);
        //排序，
        List<SendWarehouseDTO> sorted = sortSendWarehouse(filter);
        //填充
        List<SendWarehouseDTO> fill = fillSendWarehouseBeforePaging(shopId, skuCode, sorted, controlFlag, warehouses);
        //分页，条件查询
        Response<Paging<SendWarehouseDTO>> paging = pagingQuery(fill, request);
        //填充
        return fillSendWarehouseAfterPaging(paging, skuCode);
    }

    //分页后填充当前页结果
    private Response<Paging<SendWarehouseDTO>> fillSendWarehouseAfterPaging(Response<Paging<SendWarehouseDTO>> paging, String skuCode) {
        if (paging.getResult().isEmpty()) {
            return paging;
        }
        //拒单历史
        List<StockRecordLog> rejectLogs = stockRecordLogDao.findRejectHistoryOfThreeDay(skuCode, paging.getResult().getData().stream().map(SendWarehouseDTO::getWarehouseId).collect(Collectors.toList()));
        Set<Long> rejects = rejectLogs.stream().map(StockRecordLog::getWarehouseId).collect(Collectors.toSet());
        for (SendWarehouseDTO warehouseDTO : paging.getResult().getData()) {
            warehouseDTO.setHasReject(hasReject(warehouseDTO, rejects));
        }
        return paging;
    }

    //过滤可用+安全库存=0的
    private List<SendWarehouseDTO> filterInventoryIsZero(List<SendWarehouseDTO> sendWarehouseList) {
        return sendWarehouseList.stream().filter(dto-> (dto.getQuantity() + dto.getSafeQuantity()) > 0).collect(Collectors.toList());
    }
    //填充仓库信息
    private List<WarehouseDTO> fillWarehouseDTO(List<Long> warehouseIds) {
        List<List<Long>> partition = Lists.partition(warehouseIds, 100);
        List<WarehouseDTO> warehouses = Lists.newCopyOnWriteArrayList();
        partition.parallelStream().forEach(ids->{
            Response<List<WarehouseDTO>> warehouseResp = warehousesClient.findByIds(ids);
            if (!warehouseResp.isSuccess()) {
                log.error("fail to find warehouse by warehouseIds:({})", warehouseIds);
                return;
            }
            warehouses.addAll(warehouseResp.getResult());
        });
        return warehouses;
    }
    //分页前填充标签
    private List<SendWarehouseDTO> fillSendWarehouseBeforePaging(Long shopId, String skuCode, List<SendWarehouseDTO> dtoList, boolean controlFlag, List<WarehouseDTO> warehouses) {
        Map<Long, WarehouseDTO> warehousesMap = warehouses.stream().collect(Collectors.toMap(WarehouseDTO::getId, Function.identity()));
        for (SendWarehouseDTO warehouseDTO : dtoList) {
            log.info("fillSendWarehouseBeforePaging shopId:({}), skuCode:({}), warehouseDTO:({})", shopId, skuCode, warehouseDTO);
            //店仓-拒单历史
//            warehouseDTO.setHasReject(hasReject(skuCode, warehouseDTO));
            //是否排除
            warehouseDTO.setIsExclude(isExclude(shopId, skuCode, warehouseDTO, warehousesMap));
            //是否发货限制
            warehouseDTO.setIsDispatchLimit(isDispatchLimit(shopId, skuCode, warehouseDTO));

            warehouseDTO.setIsAvailable(Boolean.TRUE);
            if (controlFlag) {//根据开关判断是否可选择
                //是否排除false且是否发货限制false
                warehouseDTO.setIsAvailable(!warehouseDTO.getIsExclude() && !warehouseDTO.getIsDispatchLimit());
            }
            if (warehouseDTO.getQuantity() <= 0) {//可用库存不足，安全库存有的也，不能选择。
                warehouseDTO.setIsAvailable(false);
            }
        }

        return dtoList;
    }

    //发货限制
    private Boolean isDispatchLimit(Long shopId, String skuCode, SendWarehouseDTO warehouseDTO) {
        //店铺+sku纬度下仓库组，做白名单或黑名单
        Optional<ShopSkuSupplyRulesDTO> rule = ruleCache.getUnchecked(shopId + "-" + skuCode);
        if (!rule.isPresent()) {
            log.debug("shop sku supply rules not exist, shopId({}), skuCode({})", shopId, skuCode);
            return Boolean.FALSE;
        }

        if ("IN".equals(rule.get().getType())) {//范围内发货
            return !rule.get().getWarehouseIds().contains(warehouseDTO.getWarehouseId());
        } else { //范围外发货
            return rule.get().getWarehouseIds().contains(warehouseDTO.getWarehouseId());
        }

    }

    //商品分组关系 //这块用的配送部分的逻辑
    private Boolean isExclude(Long shopId, String skuCode, SendWarehouseDTO warehouseDTO, Map<Long, WarehouseDTO> warehousesMap) {
        Optional<SearchSkuTemplate> skuTemplate = getSkuTemplateInfo(skuCode);
        if (!skuTemplate.isPresent()) {//商品分组缺失
            log.info("skuTemplate group is null by skuCode:({})", skuCode);
            return Boolean.TRUE;
        }
        //门店商品关联缺失=true
        //仓库商品关联缺失=true
        //如果门店或仓库有排除商品，必须每个分组都要有排除商品设置=true
        //如果商品分组只配置同公司，则销售店铺和发货仓不属于同公司=true
        WarehouseDTO warehouse = warehousesMap.get(warehouseDTO.getWarehouseId());
        if (warehouse == null) {
            log.error("fail to find warehouse by warehouseId({}), ", warehouseDTO.getWarehouseId());
            return Boolean.FALSE;
        }
        OpenShop openShop = openShopCacher.findById(shopId);
        if (openShop == null) {
            log.error("fail to find openShop by shopId({})", shopId);
            return Boolean.FALSE;
        }
        String openShopCompanyCode = openShop.getExtra().get(TradeConstants.HK_COMPANY_CODE);
        Boolean canDelivery = queryHkWarhouseOrShopStockApi.canDeliveryForStock(skuTemplate.get(), /*发货仓*/warehouse,/*销售店铺*/openShopCompanyCode, Boolean.TRUE);
        return !canDelivery;
    }

    //获取商品分组
    //com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi.validateVendibleWarehouse
    private Optional<SearchSkuTemplate> getSkuTemplateInfo(String skuCode) {
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCode", skuCode);
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by skuCode:{} fail,error:{}", skuCode, response.getError());
            return Optional.empty();
        }
        if (response.getResult().getTotal() == 0) {
            return Optional.empty();
        }
        return Optional.of(response.getResult().getData().get(0));
    }

    //该店仓是否有当前商品的拒单历史
    private boolean hasReject(SendWarehouseDTO warehouseDTO, Set<Long> rejects) {
        if (1 != warehouseDTO.getWarehouseSubType()) {//只查店仓
            return false;
        }
        return rejects.contains(warehouseDTO.getWarehouseId());
    }


    //排序
    private List<SendWarehouseDTO> sortSendWarehouse(List<SendWarehouseDTO> dtoList) {
        //排序， 325总仓，总仓，325店仓, 店仓；且同级库存数大->小
//        return dtoList.stream().sorted(Comparator
//                .comparingInt(SendWarehouseDTO::getWarehouseSubType)//总仓->店仓
//                .thenComparing(sendWarehouseDTO -> "325".equals(sendWarehouseDTO.getCompanyCode()) ? -1 : 1)//325->其他
//                .thenComparing((o1, o2) -> o1.getQuantity()>o2.getQuantity() ? -1 : 1)//库存多->少
//                ).collect(Collectors.toList());
        //按库存深度,325，非325，店仓，总仓
        return dtoList.stream().sorted(Comparator.comparing(SendWarehouseDTO::getQuantity).reversed()
                .thenComparing(sendWarehouseDTO -> "325".equals(sendWarehouseDTO.getCompanyCode()) ? -1 : 1)
                .thenComparingInt(SendWarehouseDTO::getWarehouseSubType)
        ).collect(Collectors.toList());
    }

    //过滤,门店类型和状态
    private List<WarehouseDTO> filterWarehouses(List<WarehouseDTO> warehouses) {
        Response<List<Shop>> shopResp = psShopReadService.findByOuterIds(warehouses.stream().map(WarehouseDTO::getOutCode).collect(Collectors.toList()));
        if (!shopResp.isSuccess()) {
            log.error("fail to find shop cause:({})", shopResp.getError());
            return Collections.emptyList();
        }
        Map<String, Shop> shopMapping = shopResp.getResult().parallelStream().collect(Collectors.toConcurrentMap(Shop::getOuterId, Function.identity(), (a,b)->a));

        List<WarehouseDTO> result = Lists.newCopyOnWriteArrayList();
        warehouses.parallelStream().forEach(warehouse->{
            if (warehouse.getWarehouseSubType() == 1) {//店仓判断类型,综合或接单
                Shop shop = shopMapping.get(warehouse.getOutCode());
                if (shop == null || shop.getType() == null) {
                    log.info("fail to find shop by outCode({})", warehouse.getOutCode());
                    return;
                }
                if (shop.getType() == ShopType.GENERAL_SHOP.value() || shop.getType() == ShopType.RECEIVING_SHOP.value()) {
                    result.add(warehouse);
                }
            } else if (warehouse.getWarehouseSubType() == 0) {//大仓
                result.add(warehouse);
            }
        });
        return result;
    }

    //手动分页查询
    private Response<Paging<SendWarehouseDTO>> pagingQuery(List<SendWarehouseDTO> sendWarehouseList,
                                                           CommonChooseWarehouse request) {
        List<SendWarehouseDTO> filter = Lists.newArrayList();
        for (SendWarehouseDTO warehouseDTO : sendWarehouseList) {
            if (!Strings.isNullOrEmpty(request.getCompanyCode()) && !request.getCompanyCode().equals(warehouseDTO.getCompanyCode())) {
                continue;
            }
            if (!Strings.isNullOrEmpty(request.getOutCode()) && !request.getOutCode().equals(warehouseDTO.getOutCode())) {
                continue;
            }
            if (!Strings.isNullOrEmpty(request.getName()) && !warehouseDTO.getName().contains(request.getName())) {
                continue;
            }
            if (request.getExclude() != null) {
                if (request.getExclude() == 1 && !warehouseDTO.getIsExclude()) {
                    continue;
                } else if (request.getExclude() == -1 && warehouseDTO.getIsExclude()) {
                    continue;
                }
            }
            if (request.getDispatchLimit() != null) {
                if (request.getDispatchLimit() == 1 && !warehouseDTO.getIsDispatchLimit()) {
                    continue;
                } else if (request.getDispatchLimit() == -1 && warehouseDTO.getIsDispatchLimit()) {
                    continue;
                }
            }
            if (request.getAvailable() != null) {
                if (request.getAvailable() == 1 && !warehouseDTO.getIsAvailable()) {
                    continue;
                } else if (request.getAvailable() == -1 && warehouseDTO.getIsAvailable()) {
                    continue;
                }
            }
            filter.add(warehouseDTO);
        }

        if (filter.isEmpty()) {
            return Response.ok(Paging.empty());
        }

        if (request.getPageSize() == null) {
            request.setPageSize(20);
        }
        List<List<SendWarehouseDTO>> partition = Lists.partition(filter, request.getPageSize());
        log.info("[pagingQuery]  filter size({}), pageNo({})", filter.size(), request.getPageNo());
        if (request.getPageNo() == null) {
            request.setPageNo(1);
        }
        if (request.getPageNo() < 1) {
            request.setPageNo(1);
        }
        return Response.ok(new Paging<>((long) filter.size(), partition.get(request.getPageNo()-1)));
    }

    private List<SendWarehouseDTO> convert(List<WarehouseDTO> warehouses, Map<Long, AvailableInventoryDTO> availableInventoryMapping, String skuCode) {
        List<SendWarehouseDTO> result = Lists.newArrayList();
        for (WarehouseDTO warehouse : warehouses) {
            SendWarehouseDTO sendWarehouseDTO = new SendWarehouseDTO();
            sendWarehouseDTO.setWarehouseId(warehouse.getId());
            sendWarehouseDTO.setSkuCode(skuCode);
            //仓库信息
            sendWarehouseDTO.setCompanyCode(warehouse.getCompanyCode());
            sendWarehouseDTO.setOutCode(warehouse.getOutCode());
            sendWarehouseDTO.setName(warehouse.getWarehouseName());
            sendWarehouseDTO.setWarehouseCode(warehouse.getWarehouseCode());
            sendWarehouseDTO.setWarehouseSubType(warehouse.getWarehouseSubType());
            sendWarehouseDTO.setStatus(warehouse.getStatus());
            //库存信息
            int availQuantity = 0;
            int safeQuantity = 0;
            AvailableInventoryDTO inventoryDTO = availableInventoryMapping.get(warehouse.getId());
            if (inventoryDTO != null) {//不存在或负数展示0
                availQuantity = Math.max(MoreObjects.firstNonNull(inventoryDTO.getRealAvailableQuantity(), 0), 0);
                safeQuantity = Math.max(MoreObjects.firstNonNull(inventoryDTO.getSafeQuantity(), 0), 0);
            }
            sendWarehouseDTO.setQuantity(availQuantity);
            sendWarehouseDTO.setSafeQuantity(safeQuantity);
            result.add(sendWarehouseDTO);
        }
        return result;
    }

    //是否管控分组，发货限制及安全库存
    private boolean getManualDispatchRuleControl(OpenShop openShop) {
        String checkFlag = openShop.getExtra().get(TradeConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG);
        if (!"1".equals(checkFlag)) {
            return false;
        }
        String ruleFlag = openShop.getExtra().get(TradeConstants.MANUAL_DISPATCH_RULE_CONTROL);
        return "true".equals(ruleFlag);
    }

}
