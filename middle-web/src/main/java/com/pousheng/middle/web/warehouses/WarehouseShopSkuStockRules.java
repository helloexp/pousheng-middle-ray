package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.cache.SpuMaterialCacher;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopSkuRuleClient;
import com.pousheng.middle.warehouse.dto.WarehouseShopSkuStockRule;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.web.events.warehouse.PushEvent;
import com.pousheng.middle.web.user.component.UserManageShopReader;
import com.pousheng.middle.web.utils.operationlog.OperationLogIgnore;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.dto.ItemMappingCriteria;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author feisheng.ch
 * Date: 2018-05-10
 */
@RestController
@RequestMapping("/api/warehouse/shop-sku-rule")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_SHOP_RULE)
@Api(description = "店铺下商品的库存分配规则相关api")
public class WarehouseShopSkuStockRules {

    @Autowired
    private WarehouseShopSkuRuleClient warehouseShopSkuRuleClient;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @Autowired
    private UserManageShopReader userManageShopReader;
    @Autowired
    private SpuReadService spuReadService;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;
    @Autowired
    private ShopSkuStockPushHandler shopSkuStockPushHandler;
    @Autowired
    private SpuMaterialCacher spuMaterialCacher;
    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 创建商品库存推送规则
     *
     * @param warehouseShopSkuStockRule 商品库存推送规则
     * @return 新创建的规则id
     */
    @ApiOperation("创建商品库存推送规则")
    @LogMe(description = "创建商品级库存推送规则", compareTo = "warehouseShopSkuRuleClient#findById")
    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("创建")
    public Long create(@RequestBody WarehouseShopSkuStockRule warehouseShopSkuStockRule) {
        authCheck(warehouseShopSkuStockRule.getShopId());
        Response<Long> createRet = warehouseShopSkuRuleClient.createShopSkuRule(warehouseShopSkuStockRule);
        if (!createRet.isSuccess()) {
            log.error("failed to create {}, cause:{}", warehouseShopSkuStockRule, createRet.getError());
            throw new JsonResponseException(createRet.getError());
        }
        return createRet.getResult();
    }


    /**
     * 创建商品库存推送规则
     *
     * @param info 导入信息
     * @return 新创建的规则id
     */
    @ApiOperation("批量导入创建商品级库存推送规则")
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("批量导入创建商品级库存推送规则")
    public Response<Long> create(@RequestBody SkuStockRuleImportInfo info) {
        authCheck(info.getOpenShopId());
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_SHOP_SKU_RULE.toString());
        biz.setContext(mapper.toJson(info));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }


    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo 第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询导入文件的处理记录")
    @RequestMapping(value = "/import/result/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> create(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                @RequestParam(required = false, value = "pageSize") Integer pageSize) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizType(PoushengCompensateBizType.IMPORT_SHOP_SKU_RULE.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.paging(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }


    private void authCheck(Long shopId) {
        ParanaUser user = UserUtil.getCurrentUser();
        List<OpenClientShop> shops = userManageShopReader.findManageShops(user);
        List<Long> shopIds = Lists.newArrayListWithCapacity(shops.size());
        for (OpenClientShop shop : shops) {
            shopIds.add(shop.getOpenShopId());
        }
        if (!shopIds.contains(shopId)) {
            log.error("user({}) can not create shopStockRule for  shop(id={})", user, shopId);
            throw new JsonResponseException("shop.not.allowed");
        }
    }

    /**
     * 列出当前用户能查看的商品推送规则
     *
     * @param criteria 查询条件
     * @return 对应的商品库存分配规则
     */
    @ApiOperation("列出当前用户能查看的商品推送规则")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseShopSkuStockRule> pagination(ItemMappingCriteria criteria) {
        if (!StringUtils.isEmpty(criteria.getMaterialId())) {
            Map<String, String> params = Maps.newHashMap();
            params.put("spuCode", criteria.getMaterialId());
            WithAggregations<SearchSkuTemplate> result = searchByParams(params);
            if (result.getTotal() == 0) {
                return new Paging<>(0L, Lists.newArrayList());
            }
            criteria.setSkuCodes(result.getData().stream().map(SearchSkuTemplate::getSkuCode).collect(Collectors.toList()));
        }
        criteria.setStatus(1);
        Response<Paging<ItemMapping>> mappingRes = mappingReadService.paging(criteria);
        if (!mappingRes.isSuccess() || null == mappingRes.getResult()
                || null == mappingRes.getResult().getData()
                || mappingRes.getResult().getData().isEmpty()) {
            log.error("failed to find item mapping by criteria:{}, error code:{}", criteria, mappingRes.getError());

            return new Paging<>(0L, Lists.newArrayList());
        }

        Map<String, WarehouseShopSkuStockRule> ruleMap = warehouseShopSkuRuleClient.findSkuRules(criteria);
        List<String> skuCodes = mappingRes.getResult().getData().stream().filter(e -> e.getSkuCode() != null).map(ItemMapping::getSkuCode).collect(Collectors.toList());
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCodes", Joiners.COMMA.join(skuCodes));
        WithAggregations<SearchSkuTemplate> result = searchByParams(params);
        if (result.getTotal() == 0) {
            return new Paging<>(0L, Lists.newArrayList());
        }
        Map<String, String> skuCodesMaterailMap = result.getData().stream().collect(Collectors.toMap(SearchSkuTemplate::getSkuCode, SearchSkuTemplate::getSpuCode));
        Map<String, WarehouseShopSkuStockRule> rules = Maps.newHashMap();
        for (ItemMapping itemMapping : mappingRes.getResult().getData()) {
            if (StringUtils.isBlank(itemMapping.getSkuCode())) {
                continue;
            }
            if (rules.containsKey(itemMapping.getSkuCode())) {
                continue;
            }
            WarehouseShopSkuStockRule stockRule;
            WarehouseShopSkuStockRule item = ruleMap.get(itemMapping.getSkuCode());
            if (null == item) {
                stockRule = new WarehouseShopSkuStockRule();
            } else {
                stockRule = item;
            }

            stockRule.setSkuCode(itemMapping.getSkuCode());
            stockRule.setMaterialId(skuCodesMaterailMap.get(itemMapping.getSkuCode()));

            // 获取name
            if (StringUtils.isBlank(itemMapping.getItemName())) {
                // 从parana_spus表中获取
                Response<Spu> spuResponse = spuReadService.findById(itemMapping.getItemId());
                if (spuResponse.isSuccess() && null != spuResponse.getResult()) {
                    stockRule.setSkuName(spuResponse.getResult().getName());
                }
            } else {
                stockRule.setSkuName(itemMapping.getItemName());
            }

            stockRule.setShopId(criteria.getOpenShopId());

            rules.put(itemMapping.getSkuCode(), stockRule);
        }

        Paging<WarehouseShopSkuStockRule> ret = new Paging<>();
        ret.setTotal(mappingRes.getResult().getTotal());
        ret.setData(rules.values().stream().filter(Objects::nonNull).collect(Collectors.toList()));

        return ret;
    }

    /**
     * 更新商品推送规则
     *
     * @param id                        规则id
     * @param warehouseShopSkuStockRule 商品推送规则
     * @return 是否成功
     */
    @ApiOperation("更新商品推送规则")
    @LogMe(description = "更新商品级推送规则", ignore = true)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("更新")
    public Boolean update(@PathVariable Long id, @RequestBody WarehouseShopSkuStockRule warehouseShopSkuStockRule) {
        WarehouseShopSkuStockRule exist = warehouseShopSkuRuleClient.findById(id);
        if (null == exist) {
            log.error("failed to find WarehouseShopSkuStockRule(id={}), error code:{}", id);
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }

        authCheck(exist.getShopId());

        warehouseShopSkuStockRule.setId(id);
        warehouseShopSkuStockRule.setShopId(exist.getShopId());
        warehouseShopSkuStockRule.setSkuCode(exist.getSkuCode());
        Response<Boolean> updRet = warehouseShopSkuRuleClient.updateShopSkuRule(warehouseShopSkuStockRule);
        if (!updRet.isSuccess()) {
            log.error("failed to update {}, cause:{}", warehouseShopSkuStockRule, updRet.getError());
            throw new JsonResponseException(updRet.getError());
        }
        return updRet.getResult();
    }

    /**
     * 读取商品推送规则
     *
     * @param id 规则id
     * @return 是否成功
     */
    @ApiOperation("读取商品推送规则")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseShopSkuStockRule findById(@PathVariable Long id) {
        WarehouseShopSkuStockRule exist = warehouseShopSkuRuleClient.findById(id);
        if (null == exist) {
            log.error("failed to find WarehouseShopSkuStockRule(id={}), error code:{}", id);
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }

        authCheck(exist.getShopId());

        return exist;
    }

    /**
     * 推送指定商品的库存
     *
     * @param shopId  店铺id
     * @param skuCode 商品sku
     * @return 是否发送请求
     */
    @ApiOperation("推送指定商品的库存")
    @RequestMapping(value = "/push", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogIgnore
    public Boolean push(@RequestParam("shopId") Long shopId, @RequestParam("skuCode") String skuCode) {
        // 验证店铺级规则是否启用
        WarehouseShopStockRule shopRule = warehouseShopRuleClient.findByShopId(shopId);
        if (null == shopRule || shopRule.getStatus() != 1) {
            throw new JsonResponseException("warehouse.shop.rule.invalid");
        }
        //eventBus.post(new PushEvent(shopId, skuCode));
        shopSkuStockPushHandler.onPushEvent(new PushEvent(shopId, skuCode));
        return Boolean.TRUE;
    }

    private WithAggregations<SearchSkuTemplate> searchByParams(Map<String, String> params) {
        String templateName = "ps_search.mustache";
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 50, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by params:{}  fail,error:{}", params, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }
}
