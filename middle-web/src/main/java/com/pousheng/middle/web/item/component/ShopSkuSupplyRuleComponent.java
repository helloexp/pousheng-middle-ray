package com.pousheng.middle.web.item.component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRule;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleBatchCreateRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleBatchUpdateDisableRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleCreateRequest;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRuleQueryOneRequest;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Description: 商品供货规则组件
 * User: support 9
 * Date: 2018/9/17
 */
@Component
@Slf4j
public class ShopSkuSupplyRuleComponent {

    private InventoryClient inventoryClient;
    private GroupRuleCacherProxy groupRuleCacherProxy;
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    public ShopSkuSupplyRuleComponent(InventoryClient inventoryClient,
                                      GroupRuleCacherProxy groupRuleCacherProxy,
                                      SkuTemplateSearchReadService skuTemplateSearchReadService) {
        this.inventoryClient = inventoryClient;
        this.groupRuleCacherProxy = groupRuleCacherProxy;
        this.skuTemplateSearchReadService = skuTemplateSearchReadService;
    }

    public Response<Boolean> save(OpenShop openShop, SkuTemplate skuTemplate, String type) {
        if (!isSkuInShop(Lists.newArrayList(skuTemplate.getSkuCode()), openShop.getId())) {
            log.error("the skuCode:{} is not in shop:{}", skuTemplate.getSkuCode(), openShop.getId());
            return Response.fail("skuCode.not.in.shop");
        }
        ShopSkuSupplyRuleCreateRequest request = ShopSkuSupplyRuleCreateRequest.builder()
                .shopId(openShop.getId())
                .shopName(openShop.getShopName())
                .skuCode(skuTemplate.getSkuCode())
                .skuName(skuTemplate.getName())
                .materialCode(skuTemplate.getExtra().get("materialCode"))
                .type(type)
                .build();
        return inventoryClient.saveShopSkuSupplyRule(request);
    }

    public Response<Boolean> update(OpenShop openShop, SkuTemplate skuTemplate, String type, Long id) {
        if (!isSkuInShop(Lists.newArrayList(skuTemplate.getSkuCode()), openShop.getId())) {
            log.error("the skuCode:{} is not in shop:{}", skuTemplate.getSkuCode(), openShop.getId());
            return Response.fail("skuCode.not.in.shop");
        }
        ShopSkuSupplyRuleCreateRequest request = ShopSkuSupplyRuleCreateRequest.builder()
                .id(id)
                .shopId(openShop.getId())
                .shopName(openShop.getShopName())
                .skuCode(skuTemplate.getSkuCode())
                .skuName(skuTemplate.getName())
                .materialCode(skuTemplate.getExtra().get("materialCode"))
                .type(type)
                .build();
        return inventoryClient.saveShopSkuSupplyRule(request);
    }

    public Response<Boolean> save(OpenClientShop openShop, SkuTemplate skuTemplate, String type, List<String> warehouseCodes, String status) {
        return save(openShop, skuTemplate, type, warehouseCodes, status, Boolean.FALSE);
    }

    public Response<Boolean> save(OpenClientShop openShop, SkuTemplate skuTemplate, String type, List<String> warehouseCodes, String status, Boolean delta) {
        ShopSkuSupplyRuleBatchCreateRequest request = ShopSkuSupplyRuleBatchCreateRequest.builder()
                .shopId(openShop.getOpenShopId())
                .shopName(openShop.getShopName())
                .skuCode(skuTemplate.getSkuCode())
                .skuName(skuTemplate.getName())
                .materialCode(
                        MoreObjects.firstNonNull(skuTemplate.getExtra(), Collections.<String, String>emptyMap())
                                .get("materialCode"))
                .type(type)
                .warehouses(warehouseCodes)
                .status(status)
                .delta(delta)
                .build();
        return inventoryClient.batchSaveShopSkuSupplyRule(request);
    }

    public Response<ShopSkuSupplyRule> queryByShopIdAndSkuCode(Long shopId, String skuCode, String status) {
        ShopSkuSupplyRuleQueryOneRequest request = ShopSkuSupplyRuleQueryOneRequest.builder()
                .shopId(shopId)
                .skuCode(skuCode)
                .status(status)
                .build();
        return inventoryClient.queryByShopIdAndSkuCode(request);
    }

    public Boolean isSkuInShop(List<String> skuCodes, Long openShopId) {
        Set<Long> groupIds = Sets.newHashSet(groupRuleCacherProxy.findByShopId(openShopId));
        if (CollectionUtils.isEmpty(groupIds)) {
            return false;
        }
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("skuCodes", Joiners.COMMA.join(skuCodes));
        params.put("groupIds", Joiners.COMMA.join(groupIds));
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by skuCodes:{} and size:{} fail,error:{}", skuCodes, response.getError());
            return false;
        }
        if (response.getResult().getTotal() > 0) {
            return true;
        }
        return false;
    }

    public Response<Long> batchUpdateDisable(Long shopId, List<String> skuCodes, Long upperLimitId) {
        if (CollectionUtils.isEmpty(skuCodes)) {
            return Response.fail("skuCodes.is.empty");
        }
        return inventoryClient.batchUpdateDisable(ShopSkuSupplyRuleBatchUpdateDisableRequest.builder()
                .shopId(shopId).skuCodes(skuCodes).upperLimitId(upperLimitId).build());

    }

    public Response<Long> queryTopSupplyRuleId() {
        return inventoryClient.queryTopSupplyRuleId();
    }
}
