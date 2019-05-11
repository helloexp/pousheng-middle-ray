package com.pousheng.middle.web.item;

import com.google.common.base.Optional;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.common.utils.component.AzureOSSBlobClient;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.web.async.AsyncTaskExecutor;
import com.pousheng.middle.web.async.TaskResponse;
import com.pousheng.middle.web.async.supplyRule.SkuSupplyRuleDisableTask;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



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

    @Value("${item.supply.import.mq.queue.index:0}")
    public Integer queueIndex;

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
    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private BrandReadService brandReadService;
    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    /**
     * 导入模板
     */
    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public void importExcel(@RequestBody SkuStockRuleImportInfo info) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_ITEM_SUPPLY_RULE.toString());
        biz.setContext(mapper.toJson(info));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC,queueIndex);
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

    @RequestMapping(value = "/batch/disable", method = RequestMethod.POST)
    public Boolean batchDisable(Long shopId, Long brandId) {
        validation(shopId, brandId);
        Response<Boolean> response = asyncTaskExecutor.runTask(SkuSupplyRuleDisableTask.newInstance(shopId, brandId));
        if (!response.isSuccess()) {
            log.error("fail to batch disable sku supply rule, cause:{}", response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/batch/status", method = RequestMethod.GET)
    public TaskResponse showBatchDisableProcessStatus() {
        return asyncTaskExecutor.lastStatus(TaskTypeEnum.SUPPLY_RULE_BATCH_DISABLE);
    }

    private void validation(Long shopId, Long brandId) {
        OpenShop openShop = openShopCacher.findById(shopId);
        if (openShop == null) {
            log.error("fail to find shop by id:{}", shopId);
            throw new JsonResponseException("shop.not.exist");
        }
        Response<Brand> brandResponse = brandReadService.findById(brandId);
        if (!brandResponse.isSuccess()) {
            log.error("fail to find brand by id:{}, cause:{}", brandId, brandResponse.getError());
            throw new JsonResponseException(brandResponse.getError());
        }
    }
}
