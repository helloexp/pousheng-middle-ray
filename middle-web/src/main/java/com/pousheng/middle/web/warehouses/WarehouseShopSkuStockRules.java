package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
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
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
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
    public Long create(@RequestBody WarehouseShopSkuStockRule warehouseShopSkuStockRule){
        authCheck(warehouseShopSkuStockRule.getShopId());
        Response<Long> createRet = warehouseShopSkuRuleClient.createShopSkuRule(warehouseShopSkuStockRule);
        if(!createRet.isSuccess()){
            log.error("failed to create {}, cause:{}", warehouseShopSkuStockRule, createRet.getError());
            throw new JsonResponseException(createRet.getError());
        }
        return createRet.getResult();
    }

    private void authCheck(Long shopId) {
        ParanaUser user = UserUtil.getCurrentUser();
        List<OpenClientShop> shops =  userManageShopReader.findManageShops(user);
        List<Long> shopIds = Lists.newArrayListWithCapacity(shops.size());
        for (OpenClientShop shop : shops) {
            shopIds.add(shop.getOpenShopId());
        }
        if(!shopIds.contains(shopId)){
            log.error("user({}) can not create shopStockRule for  shop(id={})", user, shopId);
            throw new JsonResponseException("shop.not.allowed");
        }
    }

    /**
     * 列出当前用户能查看的商品推送规则
     *
     * @param skuCode 条码, 可选参数
     * @param pageNo 起始页面, 可选参数
     * @param pageSize 每页返回条数, 可选参数
     * @return 对应的商品库存分配规则
     */
    @ApiOperation("列出当前用户能查看的商品推送规则")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseShopSkuStockRule> pagination(@RequestParam(required = false, value = "shopId") Long shopId,
                                                        @RequestParam(required = false, value = "skuCode") String skuCode,
                                                        @RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                        @RequestParam(required = false, value = "pageSize") Integer pageSize) {

        Response<Paging<ItemMapping>> mappingRes = mappingReadService.findBy(shopId, 1,null,null,
                skuCode,null,null,
                pageNo, pageSize);
        if(!mappingRes.isSuccess() || null == mappingRes.getResult()
                || null == mappingRes.getResult().getData()
                || mappingRes.getResult().getData().isEmpty()){
            log.error("failed to find item mapping by shopId(id={}), error code:{}", shopId, mappingRes.getError());

            return new Paging<WarehouseShopSkuStockRule>(0L, Lists.newArrayList());
        }

        Map<String,WarehouseShopSkuStockRule> ruleMap = warehouseShopSkuRuleClient.findSkuRules(shopId, skuCode);

        Map<String, WarehouseShopSkuStockRule> rules = Maps.newHashMap();
        for (ItemMapping itemMapping : mappingRes.getResult().getData()) {
            if (StringUtils.isBlank(itemMapping.getSkuCode())) {
                continue;
            }
            if (rules.containsKey(itemMapping.getSkuCode())) {
                continue;
            }
            WarehouseShopSkuStockRule stockRule = null;
            WarehouseShopSkuStockRule item = ruleMap.get(itemMapping.getSkuCode());
            if (null == item) {
                stockRule = new WarehouseShopSkuStockRule();
            } else {
                stockRule = item;
            }

            stockRule.setSkuCode(itemMapping.getSkuCode());

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

            stockRule.setShopId(shopId);

            rules.put(itemMapping.getSkuCode(), stockRule);
        }

        Paging<WarehouseShopSkuStockRule> ret = new Paging<WarehouseShopSkuStockRule>();
        ret.setTotal(mappingRes.getResult().getTotal());
        ret.setData(rules.values().stream().filter(Objects::nonNull).collect(Collectors.toList()));

        return ret;
    }

    /**
     * 更新商品推送规则
     *
     * @param id 规则id
     * @param warehouseShopSkuStockRule 商品推送规则
     * @return 是否成功
     */
    @ApiOperation("更新商品推送规则")
    @LogMe(description = "更新商品级推送规则", ignore = true)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("更新")
    public Boolean update(@PathVariable Long id, @RequestBody WarehouseShopSkuStockRule warehouseShopSkuStockRule){
        WarehouseShopSkuStockRule exist = warehouseShopSkuRuleClient.findById(id);
        if(null == exist){
            log.error("failed to find WarehouseShopSkuStockRule(id={}), error code:{}", id);
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }

        authCheck(exist.getShopId());

        warehouseShopSkuStockRule.setId(id);
        warehouseShopSkuStockRule.setShopId(exist.getShopId());
        warehouseShopSkuStockRule.setSkuCode(exist.getSkuCode());
        Response<Boolean> updRet = warehouseShopSkuRuleClient.updateShopSkuRule(warehouseShopSkuStockRule);
        if(!updRet.isSuccess()){
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
    public WarehouseShopSkuStockRule findById(@PathVariable Long id){
        WarehouseShopSkuStockRule exist = warehouseShopSkuRuleClient.findById(id);
        if(null == exist){
            log.error("failed to find WarehouseShopSkuStockRule(id={}), error code:{}", id);
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }

        authCheck(exist.getShopId());

        return exist;
    }

    /**
     * 推送指定商品的库存
     *
     * @param shopId 店铺id
     * @param skuCode 商品sku
     * @return 是否发送请求
     */
    @ApiOperation("推送指定商品的库存")
    @RequestMapping(value = "/push", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogIgnore
    public Boolean push(@RequestParam("shopId")Long shopId, @RequestParam("skuCode")String skuCode){
        // 验证店铺级规则是否启用
        WarehouseShopStockRule shopRule = warehouseShopRuleClient.findByShopId(shopId);
        if (null == shopRule || shopRule.getStatus() != 1) {
            throw new JsonResponseException("warehouse.shop.rule.invalid");
        }
        //eventBus.post(new PushEvent(shopId, skuCode));
        shopSkuStockPushHandler.onPushEvent(new PushEvent(shopId, skuCode));
        return Boolean.TRUE;
    }

}
