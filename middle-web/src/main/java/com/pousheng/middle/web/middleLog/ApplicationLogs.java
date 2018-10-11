package com.pousheng.middle.web.middleLog;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.ShopWarehouseSkuStockRule;
import com.pousheng.middle.warehouse.model.PoushengChannelDTO;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopWarehouseStockRule;
import com.pousheng.middle.warehouse.model.SkuInventory;
import com.pousheng.middle.web.middleLog.dto.ApplogDto;
import com.pousheng.middle.web.middleLog.dto.ApplogTypeEnum;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.core.criteria.MemberApplicationLogCriteria;
import io.terminus.applog.core.model.MemberAppLogKey;
import io.terminus.applog.core.model.MemberApplicationLog;
import io.terminus.applog.core.service.MemberApplicationLogReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/8/20
 */
@RestController
@RequestMapping("/api/applog")
@Slf4j
public class ApplicationLogs {

    @RpcConsumer
    private MemberApplicationLogReadService memberApplicationLogReadService;

    @Autowired
    private ApplLogKeyCacher applLogKeyCacher;

    @Autowired
    private PsUserReadService psUserReadService;

    @Autowired
    private MiddleShopCacher middleShopCacher;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @ApiOperation("操作日志key")
    @RequestMapping(value = "/keys", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MemberAppLogKey> keys() {
        List<MemberAppLogKey> keys = Lists.newArrayList();
        for (String d : ApplogTypeEnum.getKeys()) {
            keys.add(applLogKeyCacher.findByDescription(d));
        }
        return keys;
    }

    @ApiOperation("操作日志列表")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ApplogDto> pagination(MemberApplicationLogCriteria criteria, @RequestParam(value = "operator", required = false) String operator) {
        if (StringUtils.isEmpty(criteria.getRootKeyId())) {
            List<String> keys = Lists.newArrayList();
            keys().stream().map(MemberAppLogKey::getId).collect(Collectors.toList()).forEach(e -> keys.add(e.toString()));
            criteria.setRootKeyIdList(keys);
        } else {
            criteria.setRootKeyIdList(Lists.newArrayList(criteria.getRootKeyId()));
        }
        if (!StringUtils.isEmpty(operator)) {
            Response<MiddleUser> userResp = psUserReadService.findByName(operator);
            if (!userResp.isSuccess() || userResp.getResult() == null) {
                return new Paging<>();
            }
            criteria.setOperatorId(userResp.getResult().getOutId().toString());
        }
        if (criteria.getCreatedEndAt() != null) {
            if (criteria.getCreatedEndAt() != null) {
                criteria.setCreatedEndAt(new DateTime(criteria.getCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
            }
        }
        Response<Paging<MemberApplicationLog>> response = memberApplicationLogReadService.paging(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        List<ApplogDto> list = Lists.newArrayList();
        for (MemberApplicationLog log : response.getResult().getData()) {
            list.add(assembleLog(log));
        }
        Paging<ApplogDto> paging = new Paging<>();
        paging.setData(list);
        paging.setTotal(response.getResult().getTotal());
        return paging;

    }

    /**
     * 组装成dto对象，
     *
     * @param memberApplicationLog
     * @return
     */
    private ApplogDto assembleLog(MemberApplicationLog memberApplicationLog) {
        ApplogDto dto = new ApplogDto();
        Map infoMap = JSON.parseObject(memberApplicationLog.getMetadata());
        dto.setCreatedAt(memberApplicationLog.getCreatedAt());
        dto.setOperator(infoMap.get("operator").toString());
        dto.setType(infoMap.get("description").toString());
        List<Object> list = JSON.parseArray(infoMap.get("contexts").toString(), Object.class);
        Map<String, String> result = new HashMap<>(10);
        ApplogTypeEnum type = ApplogTypeEnum.from(applLogKeyCacher.findById(Long.parseLong(memberApplicationLog.getRootKeyId())).getDescription());
        try {
            switch (type) {
                case SET_SAFE_STOCK:
                    Response<SkuInventory> response = inventoryClient.findInventoryById(Long.parseLong(list.get(0).toString()));
                    if (!response.isSuccess()) {
                        throw new JsonResponseException("fail.find.stock.log");
                    }
                    SkuInventory skuInventory = response.getResult();
                    result.put("warehouseName", warehouseCacher.findByCode(skuInventory.getWarehouseCode()).getWarehouseName());
                    result.put("warehouseCode", warehouseCacher.findByCode(skuInventory.getWarehouseCode()).getOutCode());
                    result.put("num", list.size() == 1 ? "0" : list.get(1).toString());
                    break;
                case SET_WAREHOUSR_SAFE_STOCK:
                    result.put("warehouseName", warehouseCacher.findById(Long.parseLong(list.get(0).toString())).getWarehouseName());
                    result.put("warehouseCode", warehouseCacher.findById(Long.parseLong(list.get(0).toString())).getOutCode());
                    result.put("num", list.size() == 1 ? "0" : list.get(1).toString());
                    break;
                case SET_CHANNEL_STOCK:
                case BATCH_SET_CHANNEL_STOCK:
                    List<PoushengChannelDTO> dtos = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), new TypeReference<List<PoushengChannelDTO>>() {
                    });
                    for (PoushengChannelDTO d : dtos) {
                        d.setWarehouseCode(warehouseCacher.findById(d.getWarehouseId()).getOutCode());
                        d.setWarehouseName(warehouseCacher.findById(d.getWarehouseId()).getWarehouseName());
                        d.setOpenShopCode(middleShopCacher.findById(d.getOpenShopId()).getExtra().get("hkPerformanceShopCode"));
                    }
                    result.put("channel_stock", JSON.toJSONString(dtos));
                    break;
                case CREATE_WAREHOUSE_SKU_PUSH_RULE:
                case UPDATE_WAREHOUSE_SKU_PUSH_RULE:
                    ShopWarehouseSkuStockRule shopWarehouseSkuStockRule = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), ShopWarehouseSkuStockRule.class);
                    shopWarehouseSkuStockRule.setShopName(middleShopCacher.findById(shopWarehouseSkuStockRule.getShopId()).getShopName());
                    shopWarehouseSkuStockRule.setOutId(middleShopCacher.findById(shopWarehouseSkuStockRule.getShopId()).getExtra().get("hkPerformanceShopCode"));
                    shopWarehouseSkuStockRule.setWarehouseName(warehouseCacher.findById(shopWarehouseSkuStockRule.getWarehouseId()).getWarehouseName());
                    shopWarehouseSkuStockRule.setWarehouseCode(warehouseCacher.findById(shopWarehouseSkuStockRule.getWarehouseId()).getOutCode());
                    Map<String, String> params = Maps.newHashMap();
                    params.put("skuCode", shopWarehouseSkuStockRule.getSkuCode());
                    WithAggregations<SearchSkuTemplate> searchResult = searchByParams(params, 10, 1);
                    if (searchResult.getTotal() > 0) {
                        shopWarehouseSkuStockRule.setMaterialId(searchResult.getData().get(0).getSpuCode());
                    }
                    result.put("warehouse_sku_rules", JSON.toJSONString(Lists.newArrayList(shopWarehouseSkuStockRule)));

                    break;
                case CREATE_WAREHOUSE_PUSH_RULE:
                case UPDATE_WAREHOUSE_PUSH_RULE:
                case BATCH_WAREHOUSE_PUSH_RULE:
                    ShopWarehouseStockRule shopWarehouseStockRule = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), ShopWarehouseStockRule.class);
                    shopWarehouseStockRule.setShopName(middleShopCacher.findById(shopWarehouseStockRule.getShopId()).getShopName());
                    shopWarehouseStockRule.setOutId(middleShopCacher.findById(shopWarehouseStockRule.getShopId()).getExtra().get("hkPerformanceShopCode"));
                    shopWarehouseStockRule.setWarehouseName(warehouseCacher.findById(shopWarehouseStockRule.getWarehouseId()).getWarehouseName());
                    shopWarehouseStockRule.setWarehouseCode(warehouseCacher.findById(shopWarehouseStockRule.getWarehouseId()).getOutCode());
                    result.put("warehouse_rules", JSON.toJSONString(Lists.newArrayList(shopWarehouseStockRule)));
                    break;

                case CREATE_SHOP_PUSH_RULE:
                case UPDATE_SHOP_PUSH_RULE:
                    ShopStockRule shopStockRule = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), ShopStockRule.class);
                    shopStockRule.setShopName(middleShopCacher.findById(shopStockRule.getShopId()).getShopName());
                    shopStockRule.setOutId(middleShopCacher.findById(shopStockRule.getShopId()).getExtra().get("hkPerformanceShopCode"));
                    result.put("shop_rule", JSON.toJSONString(shopStockRule));
                    break;

                case BATCH_WAREHOUSE_SKU_PUSH_RULE:
                    List<ShopWarehouseSkuStockRule> rules = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), new TypeReference<List<ShopWarehouseSkuStockRule>>() {
                    });
                    List<String> skuCodes = rules.stream().filter(e -> e.getSkuCode() != null).map(ShopWarehouseSkuStockRule::getSkuCode).collect(Collectors.toList());
                    Map<String, String> params1 = Maps.newHashMap();
                    params1.put("skuCodes", Joiners.COMMA.join(skuCodes));
                    WithAggregations<SearchSkuTemplate> searchResult1 = searchByParams(params1, skuCodes.size(), 1);
                    Map<String, String> skuCodesMaterailMap = searchResult1.getData().stream().collect(Collectors.toMap(SearchSkuTemplate::getSkuCode, SearchSkuTemplate::getSpuCode));

                    for (ShopWarehouseSkuStockRule d : rules) {
                        d.setShopName(middleShopCacher.findById(d.getShopId()).getShopName());
                        d.setOutId(middleShopCacher.findById(d.getShopId()).getExtra().get("hkPerformanceShopCode"));
                        d.setWarehouseName(warehouseCacher.findById(d.getWarehouseId()).getWarehouseName());
                        d.setWarehouseCode(warehouseCacher.findById(d.getWarehouseId()).getOutCode());
                        d.setMaterialId(skuCodesMaterailMap.get(d.getSkuCode()));
                        result.put("warehouse_sku_rules", JSON.toJSONString(rules));
                    }
                    break;

                default:
                    log.error("incorrect stock log type");

            }
        } catch (Exception e) {
            log.error("fail to analysis stock log context {}, cause by {}", memberApplicationLog.getMetadata(), e.getMessage());
            result = new HashMap<>();
        }
        dto.setDetail(result);
        return dto;
    }

    private WithAggregations<SearchSkuTemplate> searchByParams(Map<String, String> params, Integer pageSize, Integer pageNo) {
        String templateName = "ps_search.mustache";
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(pageNo, pageSize, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by params:{}  fail,error:{}", params, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }


}
