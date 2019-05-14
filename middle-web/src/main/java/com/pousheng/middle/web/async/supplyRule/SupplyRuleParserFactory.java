package com.pousheng.middle.web.async.supplyRule;

import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.service.TaskReadFacade;
import com.pousheng.middle.task.service.TaskWriteFacade;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.brand.service.BrandReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/6
 */
@Component
public class SupplyRuleParserFactory {

    private static TaskWriteFacade taskWriteFacade;

    private static TaskReadFacade taskReadFacade;

    private static PoushengMiddleSpuService poushengMiddleSpuService;

    private static ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;

    private static JedisTemplate jedisTemplate;

    private static SkuTemplateSearchReadService skuTemplateSearchReadService;
    @RpcConsumer
    private static BrandReadService brandReadService;
    @RpcConsumer
    private static OpenShopReadService openShopReadService;

    @Autowired
    public SupplyRuleParserFactory(TaskWriteFacade taskWriteFacade,
                                   TaskReadFacade taskReadFacade,
                                   PoushengMiddleSpuService poushengMiddleSpuService,
                                   ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent,
                                   JedisTemplate jedisTemplate,
                                   SkuTemplateSearchReadService skuTemplateSearchReadService) {
        SupplyRuleParserFactory.taskWriteFacade = taskWriteFacade;
        SupplyRuleParserFactory.taskReadFacade = taskReadFacade;
        SupplyRuleParserFactory.poushengMiddleSpuService = poushengMiddleSpuService;
        SupplyRuleParserFactory.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
        SupplyRuleParserFactory.jedisTemplate = jedisTemplate;
        SupplyRuleParserFactory.skuTemplateSearchReadService = skuTemplateSearchReadService;
    }

    public static SkuSupplyRuleDisableParser get(Long taskId, SkuSupplyRuleTaskDto param) {
        return new SkuSupplyRuleDisableParser(taskId, param,
                taskWriteFacade, taskReadFacade, poushengMiddleSpuService, shopSkuSupplyRuleComponent, jedisTemplate,
                skuTemplateSearchReadService, brandReadService, openShopReadService);
    }
}
