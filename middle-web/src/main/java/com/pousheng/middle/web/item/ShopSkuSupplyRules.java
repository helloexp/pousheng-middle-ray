package com.pousheng.middle.web.item;

import com.google.common.base.Optional;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


/**
 * Description: 店铺商品供货规则
 * User: support 9
 * Date: 2018/9/13
 */
@RestController
@RequestMapping("api/item-supply")
@Slf4j
@Api(description = "店铺商品供货规则")
public class ShopSkuSupplyRules {

    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    private OpenShopCacher openShopCacher;
    private PoushengMiddleSpuService poushengMiddleSpuService;
    private ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @Autowired
    public ShopSkuSupplyRules(PoushengCompensateBizReadService poushengCompensateBizReadService,
                              OpenShopCacher openShopCacher,
                              PoushengMiddleSpuService poushengMiddleSpuService,
                              ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent) {
        this.poushengCompensateBizReadService = poushengCompensateBizReadService;
        this.openShopCacher = openShopCacher;
        this.poushengMiddleSpuService = poushengMiddleSpuService;
        this.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
    }
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    /**
     * 导入模板
     */
    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public void importExcel(@RequestBody SkuStockRuleImportInfo info) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_ITEM_SUPPLY_RULE.toString());
        biz.setContext(mapper.toJson(info));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
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
        criteria.setBizType(PoushengCompensateBizType.IMPORT_ITEM_SUPPLY_RULE.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }


    /**
     * 创建供货规则
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public Boolean save(Long shopId, String skuCode, String type) {
       OpenShop openShop = openShopCacher.findById(shopId);
       Response<Optional<SkuTemplate>> skuTemplateResponse = poushengMiddleSpuService.findBySkuCode(skuCode);
       if (!skuTemplateResponse.isSuccess() || !skuTemplateResponse.getResult().isPresent()) {
           log.error("fail to find sku template by skuCode:{}, cause:{}", skuCode, skuTemplateResponse.getError());
           throw new JsonResponseException("no.exist.skuCode");
       }
       SkuTemplate skuTemplate = skuTemplateResponse.getResult().get();
       Response<Boolean> response = shopSkuSupplyRuleComponent.save(openShop, skuTemplate, type);
       if (!response.isSuccess() || !response.getResult()) {
           log.error("fail to save shop sku supply rule,cause:{}", response.getError());
           throw new JsonResponseException(response.getError());
       }
       return response.getResult();
    }

    /**
     * 更新供货规则
     */
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    public Boolean update(@PathVariable Long id, Long shopId, String skuCode, String type) {
        OpenShop openShop = openShopCacher.findById(shopId);
        Response<Optional<SkuTemplate>> skuTemplateResponse = poushengMiddleSpuService.findBySkuCode(skuCode);
        if (!skuTemplateResponse.isSuccess() || !skuTemplateResponse.getResult().isPresent()) {
            log.error("fail to find sku template by skuCode:{}, cause:{}", skuCode, skuTemplateResponse.getError());
            throw new JsonResponseException("no.exist.skuCode");
        }
        SkuTemplate skuTemplate = skuTemplateResponse.getResult().get();
        Response<Boolean> response = shopSkuSupplyRuleComponent.update(openShop, skuTemplate, type, id);
        if (!response.isSuccess() || !response.getResult()) {
            log.error("fail to update shop sku supply rule,cause:{}", response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

}
